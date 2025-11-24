package br.edu.icev.aed.forense;

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

        List<Alerta> resultado = new ArrayList<>();

        if (n <= 0) {
            return resultado;
        }
        Comparator<Alerta> comparatorAlertas = Comparator.comparingInt(Alerta::getSeverityLevel).reversed();

        PriorityQueue<Alerta> filaDePrioridade = new PriorityQueue<>(comparatorAlertas);

        try (BufferedReader br = new BufferedReader(new FileReader(caminhoArquivo))) {

            String linha;

            while ((linha = br.readLine()) != null) {

                String[] partes = linha.split(";");

                if (partes.length != 7) {
                    continue; // se a linha for inválida ele ignora
                }

                long timestamp = Long.parseLong(partes[0]);
                String userId = partes[1];
                String sessionId = partes[2];
                String actionType = partes[3];
                String targetResource = partes[4];
                int severity = Integer.parseInt(partes[5]);
                long bytesTransferred = Long.parseLong(partes[6]);

                Alerta alerta = new Alerta(timestamp, userId, sessionId, actionType, targetResource, severity, bytesTransferred);
                filaDePrioridade.add(alerta);
            }
        }

        // vai Extrair os n primeiros
        for (int i = 0; i < n && !filaDePrioridade.isEmpty(); i++) {

            resultado.add(filaDePrioridade.poll());
        }

        return resultado;
    }
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
