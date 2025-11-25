package br.edu.icev.aed.forense;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

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
        boolean recursoExiste = false;
        Map<String, List<String>> grafo = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(caminhoArquivo))) {
            br.readLine();
            String linha;
            String sessaoAtual = null;
            String ultimoRecurso = null;

            while ((linha = br.readLine()) != null) {
                String[] col = linha.split(",", -1);
                if (col.length < 5) continue;

                String sessao = col[2];
                String recurso = col[4];
                
                if (recurso.equals(recursoInicial)) recursoExiste = true;
                if (!sessao.equals(sessaoAtual)) {
                    sessaoAtual = sessao;
                    ultimoRecurso = recurso;
                    continue;
                }

                grafo.computeIfAbsent(ultimoRecurso, k -> new ArrayList<>()).add(recurso);
                ultimoRecurso = recurso;
            }
        }

        if (recursoInicial.equals(recursoAlvo)) {
            if (recursoExiste)
                return Optional.of(List.of(recursoInicial));
            else
                return Optional.empty();
        }

        if (!grafo.containsKey(recursoInicial))
            return Optional.empty();

        // BFS
        ArrayDeque<String> fila = new ArrayDeque<>();
        fila.add(recursoInicial);
        Map<String, String> predecessor = new HashMap<>();
        Set<String> visitado = new HashSet<>();
        visitado.add(recursoInicial);

        while (!fila.isEmpty()) {
            String atual = fila.poll();
            List<String> vizinhos = grafo.get(atual);
            if (vizinhos == null) continue;
            for (String nxt : vizinhos) {
                if (!visitado.add(nxt)) continue;
                predecessor.put(nxt, atual);
                if (nxt.equals(recursoAlvo)) {
                    ArrayList<String> caminho = new ArrayList<>();
                    String p = nxt;
                    while (p != null) {
                        caminho.add(p);
                        p = predecessor.get(p);
                    }
                    
                    Collections.reverse(caminho);
                    return Optional.of(caminho);
                }
                fila.add(nxt);
            }
        }
        return Optional.empty();
    }
}