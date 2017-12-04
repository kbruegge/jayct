package reconstruction.containers;

/**
 * Created by mackaiver on 04.12.17.
 */
public class DL3 {

    public final double particlePrediction;
    public final double energyPrediction;
    public final ReconstrucedEvent reconstrucedEvent;

    public DL3(double particlePrediction, double energyPrediction, ReconstrucedEvent reconstrucedEvent) {
        this.particlePrediction = particlePrediction;
        this.energyPrediction = energyPrediction;
        this.reconstrucedEvent = reconstrucedEvent;
    }
}
