package br.edu.icev.aed.forense;

import java.util.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class SolucaoForense implements AnaliseForenseAvancada {

    @Override
    public Set<String> encontrarSessoesInvalidas(String caminhoArquivo) throws IOException {
        Map<String, Deque<String>> pilhas = new HashMap<>();
        Set<String> invalidas = new HashSet<>();

        try (BufferedReader br = new BufferedReader(new FileReader(caminhoArquivo))) {
            br.readLine();
            String linha;
            while ((linha = br.readLine()) != null) {

                String[] col = linha.split(",", -1);

                String user = col[1];
                String sessao = col[2];
                String acao = col[3];

                Deque<String> pilha = pilhas.get(user);
                if (pilha == null) {
                    pilha = new ArrayDeque<>(4);
                    pilhas.put(user, pilha);
                }
                char c = acao.charAt(0);

                if (c == 'L' && acao.equals("LOGIN")) {
                    if (!pilha.isEmpty()) {
                        invalidas.add(sessao);
                    }
                    pilha.push(sessao);
                }
                else {
                    if (pilha.isEmpty()) {
                        invalidas.add(sessao);
                    } else {
                        String topo = pilha.pop();
                        if (!topo.equals(sessao)) {
                            invalidas.add(topo);
                            invalidas.add(sessao);
                        }
                    }
                }
            }
        }

        for (Deque<String> p : pilhas.values()) {
            invalidas.addAll(p);
        }
        return new TreeSet<>(invalidas);
    }

    @Override
    public List<String> reconstruirLinhaTempo(String caminhoArquivoCsv, String sessionId) throws IOException {
        List<String> linha = new ArrayList<>(32);
        try (BufferedReader br = new BufferedReader(new FileReader(caminhoArquivoCsv))) {
            br.readLine();
            String row;

            while ((row = br.readLine()) != null) {
                String[] col = row.split(",", -1);
                if (sessionId.equals(col[2])) {
                    linha.add(col[3]);
                }
            }
        }

        return linha;
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
