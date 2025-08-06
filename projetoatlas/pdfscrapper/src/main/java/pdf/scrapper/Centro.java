package pdf.scrapper;

public record Centro(String acronimo, String nome, String website) {
    /** Retorna CSV formatado. */
    public String toCSV() {
        return String.format("\"%s\",\"%s\",\"%s\"",
                             acronimo, nome, website);
    }

    /** Chave única para deduplicação. */
    public String toKey() {
        return acronimo + "|" + nome;
    }
}
