package br.edu.icev.aed.forense;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class SolucaoForense implements AnaliseForenseAvancada {
    private volatile List<Alerta> alertasCache = null;
    private volatile String cachePath = null;

    @Override
    public Set<String> encontrarSessoesInvalidas(String caminhoArquivo) throws IOException {
        Map<String, Deque<String>> pilhas = new HashMap<>();
        Set<String> invalidas = new HashSet<>();

        try (BufferedReader br = new BufferedReader(new FileReader(caminhoArquivo))) {
            br.readLine();
            String linha;
            while ((linha = br.readLine()) != null) {
                String[] col = linha.split(",", -1);
                if (col.length < 4) continue;

                String user = col[1].trim();
                String sessao = col[2].trim();
                String acao = col[3].trim();
                if (user.isEmpty() || sessao.isEmpty() || acao.isEmpty()) continue;

                Deque<String> pilha = pilhas.get(user);
                if (pilha == null) {
                    pilha = new ArrayDeque<>(4);
                    pilhas.put(user, pilha);
                }
                char c = acao.charAt(0);
                if (c == 'L' && "LOGIN".equals(acao)) {
                    if (!pilha.isEmpty()) {
                        invalidas.add(sessao);
                    }
                    pilha.push(sessao);
                } else if ("LOGOUT".equals(acao) || "SESSION_EXPIRED".equals(acao)) {
                    if (pilha.isEmpty()) {
                        invalidas.add(sessao);
                    } else {
                        String topo = pilha.pop();
                        if (!topo.equals(sessao)) {
                            invalidas.add(topo);
                            invalidas.add(sessao);
                        }
                    }
                } else {
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
        if (sessionId == null) return linha;
        try (BufferedReader br = new BufferedReader(new FileReader(caminhoArquivoCsv))) {
            br.readLine();
            String row;
            while ((row = br.readLine()) != null) {
                String[] col = row.split(",", -1);
                if (col.length < 4) continue;
                if (sessionId.equals(col[2].trim())) {
                    linha.add(col[3].trim());
                }
            }
        }

        return linha;
    }

    @Override
    public List<Alerta> priorizarAlertas(String caminhoArquivo, int n) throws IOException {
        if (n <= 0) return Collections.emptyList();
        List<Alerta> all = getAlertasCached(caminhoArquivo);
        if (all.isEmpty()) return Collections.emptyList();
        Comparator<Alerta> keepComparator = Comparator
                .comparingInt(Alerta::getSeverityLevel)
                .thenComparingLong(Alerta::getBytesTransferred);

        PriorityQueue<Alerta> minHeap = new PriorityQueue<>(Math.min(n, all.size()), keepComparator);

        for (Alerta a : all) {
            if (minHeap.size() < n) {
                minHeap.offer(a);
            } else if (keepComparator.compare(a, minHeap.peek()) > 0) {
                minHeap.poll();
                minHeap.offer(a);
            }
        }
        List<Alerta> resultado = new ArrayList<>(minHeap);
        resultado.sort(Comparator
                .comparingInt(Alerta::getSeverityLevel).reversed()
                .thenComparingLong(Alerta::getBytesTransferred).reversed()
                .thenComparingLong(Alerta::getTimestamp)); // tie-breaker stable
        return resultado;
    }

    @Override
    public Map<Long, Long> encontrarPicosTransferencia(String caminhoArquivo) throws IOException {
        List<Alerta> alertas = getAlertasCached(caminhoArquivo);
        int size = alertas.size();
        if (size == 0) return Collections.emptyMap();

        Map<Long, Long> resultado = new HashMap<>();
        ArrayDeque<Alerta> stack = new ArrayDeque<>(64);
        for (int i = size - 1; i >= 0; i--) {
            Alerta atual = alertas.get(i);
            long bytes = atual.getBytesTransferred();

            while (!stack.isEmpty() && stack.peek().getBytesTransferred() <= bytes) {
                stack.pop();
            }
            if (!stack.isEmpty()) {
                resultado.put(atual.getTimestamp(), stack.peek().getTimestamp());
            }
            stack.push(atual);
        }

        return resultado;
    }
    private List<Alerta> getAlertasCached(String caminhoArquivo) throws IOException {
        if (caminhoArquivo == null) return Collections.emptyList();

        if (caminhoArquivo.equals(cachePath) && alertasCache != null) {
            return alertasCache;
        }

        synchronized (this) {
            if (caminhoArquivo.equals(cachePath) && alertasCache != null) {
                return alertasCache;
            }
            List<Alerta> parsed = lerAlertasDoArquivo(caminhoArquivo);
            alertasCache = parsed;
            cachePath = caminhoArquivo;
            return parsed;
        }
    }

    private List<Alerta> lerAlertasDoArquivo(String caminhoArquivo) throws IOException {
        List<Alerta> lista = new ArrayList<>(2048);

        try (BufferedReader br = new BufferedReader(new FileReader(caminhoArquivo))) {
            br.readLine();
            String linha;
            while ((linha = br.readLine()) != null) {
                String[] p = linha.split(",", -1);
                if (p.length < 7) continue;
                String tsS = p[0].trim();
                String user = p[1].trim();
                String session = p[2].trim();
                String action = p[3].trim();
                String resource = p[4].trim();
                String sevS = p[5].trim();
                String bytesS = p[6].trim();

                if (tsS.isEmpty() || user.isEmpty() || session.isEmpty()) continue;
                try {
                    long ts = Long.parseLong(tsS);
                    int sev = sevS.isEmpty() ? 0 : Integer.parseInt(sevS);
                    long bytes = bytesS.isEmpty() ? 0L : Long.parseLong(bytesS);

                    lista.add(new Alerta(ts, user, session, action, resource, sev, bytes));
                } catch (NumberFormatException ex) {
                }
            }
        }
        return lista;
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