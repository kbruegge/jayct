import hexmap.TelescopeArray;
import io.ImageReader;
import ml.TreeEnsemblePredictor;
import ml.Vectorizer;
import reconstruction.DirectionReconstruction;
import reconstruction.HillasParametrization;
import reconstruction.TailCut;
import reconstruction.containers.DL3;
import reconstruction.containers.Moments;
import reconstruction.containers.ReconstrucedEvent;
import reconstruction.containers.ShowerImage;

import java.util.List;
import java.util.Optional;

/**
 * Run full analysis chain. Do I want this?
 * Created by mackaiver on 04.12.17.
 */
class FullChain {

    final private TreeEnsemblePredictor separationModel;

    FullChain(TreeEnsemblePredictor separationModel) {
        this.separationModel = separationModel;
    }

    Optional<DL3> analyze(ImageReader.Event event){

        List<ShowerImage> showerImages = TailCut.onImagesInEvent(event);
        List<Moments> moments = HillasParametrization.fromShowerImages(showerImages);

        ReconstrucedEvent reconstrucedEvent = DirectionReconstruction.fromMoments(moments, event.mc.alt, event.mc.az);

        if (reconstrucedEvent.direction.isNaN()){
            return Optional.empty();
        }

        double predictionParticle = moments.stream()
                .map(m ->
                        new Vectorizer().of(
                                event.array.numTriggeredTelescopes,
                                m.numberOfPixel,
                                m.width,
                                m.length,
                                m.skewness,
                                m.kurtosis,
                                m.phi,
                                m.miss,
                                m.size,
                                TelescopeArray.cta().telescopeFromId(m.telescopeID).telescopeType.ordinal()
                        ).createFloatVector()
                )
                .mapToDouble(f ->
                        (double) separationModel.predictProba(f)[0]
                )
                .average()
                .orElse(0);

        return Optional.of(new DL3(predictionParticle, Double.NaN, reconstrucedEvent));
    }

}
