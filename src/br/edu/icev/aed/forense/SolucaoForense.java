package br.edu.icev.aed.forense;

import java.io.IOException;
import java.util.*;
import java.io.BufferedReader;
import java.io.FileReader;

public class SolucaoForense implements AnaliseForenseAvancada {

    @Override
    public Set<String> encontrarSessoesInvalidas(String caminhoArquivo) throws IOException {
        return Collections.emptySet();
    }

    @Override
    public List<String> reconstruirLinhaTempo(String caminhoArquivo, String sessionId) throws IOException {
        return Collections.emptyList();
    }

    @Override
    public List<Alerta> priorizarAlertas(String caminhoArquivo, int n) throws IOException {

        List<Alerta> resultado = new ArrayList<>();

        if (n <= 0) {
            return resultado;
        }

        Comparator<Alerta> comparatorAlertas = Comparator.comparingInt(Alerta::getSeverityLevel).reversed();
        PriorityQueue<Alerta> filaDePrioridade = new PriorityQueue<>(comparatorAlertas);

        List<Alerta> alertas = lerAlertasDoArquivo(caminhoArquivo);
        filaDePrioridade.addAll(alertas);

        for (int i = 0; i < n && !filaDePrioridade.isEmpty(); i++) {
            resultado.add(filaDePrioridade.poll());
        }

        return resultado;
    }



    @Override
    public Map<Long, Long> encontrarPicosTransferencia(String caminhoArquivo) throws IOException {

        List<Alerta> alertas = lerAlertasDoArquivo(caminhoArquivo);

        if (alertas == null || alertas.isEmpty()) {
            return new HashMap<>();
        }

        Map<Long, Long> resultado = new HashMap<>();

        Stack<Alerta> stack = new Stack<>();

        for (int i = alertas.size() - 1; i >= 0; i--) {
            Alerta eventoAtual = alertas.get(i);

            while (!stack.isEmpty() && stack.peek().getBytesTransferred() <= eventoAtual.getBytesTransferred()) {
                stack.pop();
            }
            if (!stack.isEmpty()) {
                resultado.put(eventoAtual.getTimestamp(), stack.peek().getTimestamp());
            }

            stack.push(eventoAtual);
        }

        return resultado;
    }
private List<Alerta> lerAlertasDoArquivo(String caminhoArquivo) throws IOException {
    List<Alerta> lista = new ArrayList<>();

    try (BufferedReader br = new BufferedReader(new FileReader(caminhoArquivo))) {
        String linha;
        while ((linha = br.readLine()) != null) {

            String[] partes = linha.split(";"); //talvez precise mudar daqui esse ponto e vírgula

            if (partes.length != 7) {
                continue;
            }

            long timestamp = Long.parseLong(partes[0]);
            String userId = partes[1];
            String sessionId = partes[2];
            String actionType = partes[3];
            String targetResource = partes[4];
            int severity = Integer.parseInt(partes[5]);
            long bytesTransferred = Long.parseLong(partes[6]);

            Alerta alerta = new Alerta(timestamp, userId, sessionId, actionType, targetResource, severity, bytesTransferred);
            lista.add(alerta);
        }
    }
    return lista;
}

    @Override
    public Optional<List<String>> rastrearContaminacao(String caminhoArquivo, String recursoInicial, String recursoAlvo) throws IOException {
        return Optional.empty();
    }
}
