package br.edu.icev.aed.forense;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

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
        return Collections.emptyList();
    }

    @Override
    public Map<Long, Long> encontrarPicosTransferencia(String caminhoArquivo) throws IOException {
        return Collections.emptyMap();
    }

    @Override
    public Optional<List<String>> rastrearContaminacao(String caminhoArquivo, String recursoInicial, String recursoAlvo) throws IOException {
        if (recursoInicial.equals(recursoAlvo)) {
            try (var br = new BufferedReader(new FileReader(caminhoArquivo))) {
                br.readLine();
                String linha;
                while ((linha = br.readLine()) != null) {
                    String[] col = linha.split(",", -1);
                    if (col.length >= 5 && col[4].equals(recursoInicial)) {
                        return Optional.of(List.of(recursoInicial));
                    }
                }
            }
            return Optional.empty();
        }
        
        Map<String, List<String>> grafo = new HashMap<>();
        Map<String, List<String[]>> porSessao = new HashMap<>();

        try (var br = new BufferedReader(new FileReader(caminhoArquivo))) {
            br.readLine();
            String linha;
            while ((linha = br.readLine()) != null) {
                String[] col = linha.split(",", -1);
                if (col.length < 5) continue;
                String sessionId = col[2];
                porSessao.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(col);
            }
        }

        for (var entrada : porSessao.entrySet()) {
            List<String[]> logs = entrada.getValue();

            if (logs.size() < 2) continue;
            for (int i = 0; i < logs.size() - 1; i++) {
                String recursoA = logs.get(i)[4];
                String recursoB = logs.get(i + 1)[4];
                grafo.computeIfAbsent(recursoA, k -> new ArrayList<>()).add(recursoB);
            }
        }

        //BFS
        if (!grafo.containsKey(recursoInicial)) return Optional.empty();
        Queue<String> fila = new LinkedList<>();
        fila.add(recursoInicial);

        Map<String, String> predecessor = new HashMap<>();
        Set<String> visitado = new HashSet<>();
        visitado.add(recursoInicial);
        boolean encontrado = false;

        while (!fila.isEmpty()) {
            String atual = fila.poll();

            if (!grafo.containsKey(atual)) continue;
            for (String vizinho : grafo.get(atual)) {
                if (!visitado.contains(vizinho)) {
                    visitado.add(vizinho);
                    predecessor.put(vizinho, atual);
                    if (vizinho.equals(recursoAlvo)) {
                        encontrado = true;
                        break;
                    }
                    fila.add(vizinho);
                }
            }
            if (encontrado) break;

        }

        if (!encontrado) return Optional.empty();
        List<String> caminho = new ArrayList<>();
        String atual = recursoAlvo;
        while (atual != null) {
            caminho.add(atual);
            atual = predecessor.get(atual);
        }

        Collections.reverse(caminho); 
        return Optional.of(caminho);
    }
}
