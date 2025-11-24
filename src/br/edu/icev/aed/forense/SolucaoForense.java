package br.edu.icev.aed.forense;

import java.util.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class SolucaoForense implements AnaliseForenseAvancada {

    private List<String[]> carregarLogsDoArquivo(String caminhoArquivo) throws IOException {
        List<String[]> todosLogs = new ArrayList<>();
        try (BufferedReader leitor = new BufferedReader(new FileReader(caminhoArquivo))) {
            leitor.readLine();
            String linhaAtual;
            while ((linhaAtual = leitor.readLine()) != null) {
                String[] campos = linhaAtual.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                for (int i = 0; i < campos.length; i++) {
                    campos[i] = campos[i].replace("\"", "").trim();
                }
                if (campos.length >= 7) {
                    todosLogs.add(campos);
                }
            }
        }
        return todosLogs;
    }

    @Override
    public Set<String> encontrarSessoesInvalidas(String caminhoArquivoCsv) throws IOException {
        List<String[]> logs = carregarLogsDoArquivo(caminhoArquivoCsv);
        Map<String, Deque<String>> pilhaDeSessoesPorUsuario = new HashMap<>();
        Set<String> sessoesConsideradasInvalidas = new HashSet<>();

        for (String[] registro : logs) {
            String idUsuario = registro[1];
            String idSessao = registro[2];
            String tipoAcao = registro[3];

            Deque<String> pilhaDoUsuario = pilhaDeSessoesPorUsuario
                    .computeIfAbsent(idUsuario, k -> new ArrayDeque<>());

            if ("LOGIN".equals(tipoAcao)) {
                if (!pilhaDoUsuario.isEmpty()) {
                    sessoesConsideradasInvalidas.add(idSessao);
                }
                pilhaDoUsuario.push(idSessao);
            }
            else if ("LOGOUT".equals(tipoAcao) || "SESSION_EXPIRED".equals(tipoAcao)) {
                if (pilhaDoUsuario.isEmpty()) {
                    sessoesConsideradasInvalidas.add(idSessao);
                } else {
                    String sessaoAbertaAtual = pilhaDoUsuario.pop();
                    if (!sessaoAbertaAtual.equals(idSessao)) {
                        sessoesConsideradasInvalidas.add(idSessao);
                        sessoesConsideradasInvalidas.add(sessaoAbertaAtual);
                        pilhaDoUsuario.push(sessaoAbertaAtual);
                    }
                }
            }
        }

        for (Deque<String> pilhaRestante : pilhaDeSessoesPorUsuario.values()) {
            sessoesConsideradasInvalidas.addAll(pilhaRestante);
        }

        return new TreeSet<>(sessoesConsideradasInvalidas);
    }

    @Override
    public List<String> reconstruirLinhaTempo(String caminhoArquivoCsv, String sessionId) throws IOException {
        List<String[]> logs = carregarLogsDoArquivo(caminhoArquivoCsv);
        Queue<String> filaDeAcoesDaSessao = new LinkedList<>();

        for (String[] registro : logs) {
            if (sessionId.equals(registro[2])) {
                filaDeAcoesDaSessao.offer(registro[3]);
            }
        }

        List<String> linhaDoTempoFinal = new ArrayList<>();
        while (!filaDeAcoesDaSessao.isEmpty()) {
            linhaDoTempoFinal.add(filaDeAcoesDaSessao.poll());
        }

        return linhaDoTempoFinal;
    }

    @Override
    public List<Alerta> priorizarAlertas(String caminhoArquivo, int n) throws IOException {
        return Collections.emptyList();
    }

    @Override
    public Map<Long, Long> encontrarPicosTransferencia(String caminhoArquivo) throws IOException {
        return Collections.emptyMap();
    }

    @Override
    public Optional<List<String>> rastrearContaminacao(String caminhoArquivo, String recursoInicial, String recursoAlvo) throws IOException {
        return Optional.empty();
    }
}
