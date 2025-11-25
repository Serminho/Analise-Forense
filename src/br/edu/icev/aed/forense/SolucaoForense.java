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
        if (n <= 0) return Collections.emptyList();
        List<Alerta> alertas = lerAlertasDoArquivo(caminhoArquivo);
        if (alertas.isEmpty()) return Collections.emptyList();
        PriorityQueue<Alerta> pq =
                new PriorityQueue<>(Comparator.comparingInt(Alerta::getSeverityLevel).reversed());
        pq.addAll(alertas);
        List<Alerta> resultado = new ArrayList<>(n);

        for (int i = 0; i < n && !pq.isEmpty(); i++)
            resultado.add(pq.poll());
        return resultado;
    }

    @Override
    public Map<Long, Long> encontrarPicosTransferencia(String caminhoArquivo) throws IOException {
        List<Alerta> alertas = lerAlertasDoArquivo(caminhoArquivo);
        int size = alertas.size();
        if (size == 0) return Collections.emptyMap();

        Map<Long, Long> resultado = new HashMap<>();
        ArrayDeque<Alerta> stack = new ArrayDeque<>();
        for (int i = size - 1; i >= 0; i--) {

            Alerta atual = alertas.get(i);
            long bytes = atual.getBytesTransferred();

            while (!stack.isEmpty() && stack.peek().getBytesTransferred() <= bytes)
                stack.pop();
            if (!stack.isEmpty())
                resultado.put(atual.getTimestamp(), stack.peek().getTimestamp());

            stack.push(atual);
        }

        return resultado;
    }
    
    private List<Alerta> lerAlertasDoArquivo(String caminhoArquivo) throws IOException {
        List<Alerta> lista = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(caminhoArquivo))) {
            br.readLine();
            String linha;
            while ((linha = br.readLine()) != null) {

                String[] p = linha.split(",", -1);
                if (p.length < 7) continue;
                try {
                    long ts = Long.parseLong(p[0].trim());
                    int sev = Integer.parseInt(p[5].trim());
                    long bytes = Long.parseLong(p[6].trim());

                    lista.add(new Alerta(
                            ts,
                            p[1].trim(),
                            p[2].trim(),
                            p[3].trim(),
                            p[4].trim(),
                            sev,
                            bytes
                    ));

                } catch (NumberFormatException ignored) {
                }
            }
        }
        return lista;
    }

    @Override
    public Optional<List<String>> rastrearContaminacao(String caminhoArquivo, String recursoInicial, String recursoAlvo) throws IOException {
        return Optional.empty();
    }
}
