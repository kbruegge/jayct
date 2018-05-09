import Pyro4

from ctapipe.reco import HillasReconstructor
from ctapipe.image.hillas import hillas_parameters
from ctapipe.image.cleaning import tailcuts_clean
import numpy as np
import pickle
from collections import namedtuple
import astropy.units as units
import warnings
from astropy.utils.exceptions import AstropyDeprecationWarning
import traceback
import os

# do some horrible things to silencece astropy warnings in ctapipe
warnings.filterwarnings('ignore', category=AstropyDeprecationWarning, append=True)
warnings.filterwarnings('ignore', category=FutureWarning, append=True)

SubMomentParameters = namedtuple('SubMomentParameters', 'size,cen_x,cen_y,length,width,psi')

dirname = os.path.dirname(__file__)
filename = os.path.join(dirname, "./instrument_description.pkl")


def dummy_function_h_max(self, hillas_dict, subarray, tel_phi):
    return -1


@Pyro4.expose
class Reconstructor():
    instrument = pickle.load(open(filename, 'rb'))

    def reconstruct_direction(self, moments):
        reco = HillasReconstructor()
        # monkey patch this huansohn. this is super slow otherwise. who needs max h anyways
        reco.fit_h_max = dummy_function_h_max

        params = {}
        pointing_azimuth = {}
        pointing_altitude = {}
        # TODO: what about array event id?
        for row in moments:
            tel_id = row["telescopeID"]
            # the data in each event has to be put inside these namedtuples to call reco.predict
            # TODO: what is psi? phi or delta?
            moment = SubMomentParameters(size=row["size"], cen_x=row["meanX"] * units.m,
                                         cen_y=row["meanY"] * units.m,
                                         length=row["length"] * units.m,
                                         width=row["width"] * units.m, psi=row["phi"] * units.deg)
            params[tel_id] = moment
            pointing_azimuth[tel_id] = 0 * units.rad  # row.pointing_azimuth * units.rad
            pointing_altitude[
                tel_id] = 1.2217304763960306 * units.rad  # row.pointing_altitude * units.rad

        try:
            reconstruction = reco.predict(params, self.instrument, pointing_azimuth,
                                          pointing_altitude)
        except NameError:
            return {'alt_prediction': np.nan,
                    'az_prediction': np.nan,
                    'core_x_prediction': np.nan,
                    'core_y_prediction': np.nan,
                    # 'array_event_id': array_event_id,
                    }

        if reconstruction.alt.si.value == np.nan:
            print('Not reconstructed')
            print(params)

        return {'alt_prediction': ((np.pi / 2) - reconstruction.alt.si.value),
                # TODO srsly now? FFS
                'az_prediction': reconstruction.az.si.value,
                'core_x_prediction': reconstruction.core_x.si.value,
                'core_y_prediction': reconstruction.core_y.si.value,
                # 'array_event_id': array_event_id,
                # 'h_max_prediction': reconstruction.h_max.si.value
                }

    def ping(self, something):
        return something

    def tail_cut(self, input):
        # Apply image cleaning
        try:
            image = input["image"]
            id = input["cameraId"]
            geom = self.instrument.subarray.tel[id].camera
            image = np.array(image)
            cleanmask = tailcuts_clean(geom, image, picture_thresh=10, boundary_thresh=5)
            index = [i for i, x in enumerate(cleanmask) if x]
            return index, list(image[cleanmask])
        except:
            print("TAILCUT ERROR:")
            print(traceback.format_exc())
            id = input["cameraId"]
            geom = self.instrument.subarray.tel[id].camera
            print(geom)
            result = list(np.zeros(1)), list(np.zeros(1))
            return result

    def hillas(self, input):
        # Calculate image parameters
        try:
            image = input["image"]
            id = input["cameraId"]
            geom = self.instrument.subarray.tel[id].camera
            image = np.array(image)
            hillas = hillas_parameters(geom, image)

            d = {'size': hillas.size,
                 'width': hillas.width.value,
                 'length': hillas.length.value,
                 'cen_x': hillas.cen_x.value,
                 'cen_y': hillas.cen_y.value,
                 'r': hillas.r.value,
                 'psi': hillas.psi.value,
                 'phi': hillas.phi.value,
                 'skewness': hillas.skewness,
                 'kurtosis': hillas.kurtosis,
                 'miss': hillas.miss.value,
                 'eventId': input["eventId"],
                 'cameraId': input["cameraId"]
                 }
            return d
        except:
            print("HILLAS PARAMETRIZATION EXCEPTION")
            print(traceback.format_exc())
            return {'size': np.nan,
                    'width': np.nan,
                    'length': np.nan,
                    'cen_x': np.nan,
                    'cen_y': np.nan,
                    'r': np.nan,
                    'psi': np.nan,
                    'phi': np.nan,
                    'skewness': np.nan,
                    'kurtosis': np.nan,
                    'miss': np.nan,
                    'eventId': input["eventId"],
                    'cameraId': input["cameraId"]
                    }


def main():
    Pyro4.Daemon.serveSimple(
            {
                Reconstructor: 'streams.processors',
            },
            ns=True
    )
    print('Pyro daemon running.')


if __name__ == '__main__':
    main()
