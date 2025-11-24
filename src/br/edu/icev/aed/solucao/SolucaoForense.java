package br.edu.icev.aed.solucao;

import br.edu.icev.aed.forense.AnaliseForenseAvancada;
import br.edu.icev.aed.forense.Alerta;

import java.io.IOException;
import java.util.*;

public class SolucaoForense implements AnaliseForenseAvancada {

    private List<String[]> ler(String caminho) throws IOException {
        var linhas = new ArrayList<String[]>();
        try (var br = new BufferedReader(new FileReader(caminho))) {
            br.readLine();
            String l;
            while ((l = br.readLine()) != null) {
                String[] c = l.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                for (int i = 0; i < c.length; i++) c[i] = c[i].replace("\"", "").trim();
                if (c.length >= 6) linhas.add(c);
            }
        }
        return linhas;
    }

 @Override
    public Set<String> encontrarSessoesInvalidas(String caminhoArquivoCsv) throws IOException {
        var logs = ler(caminhoArquivoCsv);
        var cont = new HashMap<String, Integer>();

        for (String[] log : logs) {
            String sessao = log[1];
            String acao   = log[3];

            cont.putIfAbsent(sessao, 0);
            if ("LOGIN".equals(acao)) {
                cont.put(sessao, cont.get(sessao) + 1);
            } else if ("LOGOUT".equals(acao) || "SESSION_EXPIRED".equals(acao)) {
                int v = cont.get(sessao);
                if (v > 0) cont.put(sessao, v - 1);
            }
        }

        var invalidas = new TreeSet<String>();
        for (var e : cont.entrySet()) {
            if (e.getValue() > 0) invalidas.add(e.getKey());
        }
        return invalidas;
    }

    @Override
    public List<String> reconstruirLinhaTempo(String caminhoArquivoCsv, String sessionId) throws IOException {
        var logs = ler(caminhoArquivoCsv);
        var resultado = new ArrayList<String>();

        for (String[] log : logs) {
            if (sessionId.equals(log[1])) {
                resultado.add(log[3] + " -> " + log[4]);
            }
        }
        return resultado;
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
