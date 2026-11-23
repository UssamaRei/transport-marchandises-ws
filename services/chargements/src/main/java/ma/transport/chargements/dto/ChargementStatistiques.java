package ma.transport.chargements.dto;

public class ChargementStatistiques {
    private final long total;
    private final long brouillons;
    private final long valides;

    public ChargementStatistiques(long total, long brouillons, long valides) {
        this.total = total;
        this.brouillons = brouillons;
        this.valides = valides;
    }

    public long getTotal() { return total; }
    public long getBrouillons() { return brouillons; }
    public long getValides() { return valides; }
}
