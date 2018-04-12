from ctapipe.reco import HillasReconstructor
import pickle
from collections import namedtuple
import astropy.units as units
import warnings
from astropy.utils.exceptions import AstropyDeprecationWarning
import numpy as np
import os
import Pyro4

# do some horrible things to silencece astropy warnings in ctapipe
warnings.filterwarnings('ignore', category=AstropyDeprecationWarning, append=True)
warnings.filterwarnings('ignore', category=FutureWarning, append=True)

SubMomentParameters = namedtuple('SubMomentParameters', 'size,cen_x,cen_y,length,width,psi')

dirname = os.path.dirname(__file__)
filename = os.path.join(dirname, "../instrument_description.pkl")

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
        #TODO: what about array event id?
        for row in moments:
            tel_id = row["telescopeID"]
            # the data in each event has to be put inside these namedtuples to call reco.predict
            #TODO: what is psi? phi or delta?
            moment = SubMomentParameters(size=row["size"], cen_x=row["meanX"] * units.m,
                                          cen_y=row["meanY"] * units.m, length=row["length"] * units.m,
                                          width=row["width"] * units.m, psi=row["phi"] * units.deg)
            params[tel_id] = moment
            pointing_azimuth[tel_id] = 0 * units.rad # row.pointing_azimuth * units.rad
            pointing_altitude[tel_id] = 1.2217304763960306 * units.rad #row.pointing_altitude * units.rad

        try:
            reconstruction = reco.predict(params, self.instrument, pointing_azimuth, pointing_altitude)
        except NameError:
            return {'alt_prediction': np.nan,
                    'az_prediction': np.nan,
                    'core_x_prediction': np.nan,
                    'core_y_prediction': np.nan,
                    #'array_event_id': array_event_id,
                    }

        if reconstruction.alt.si.value == np.nan:
            print('Not reconstructed')
            print(params)

        return {'alt_prediction': ((np.pi / 2) - reconstruction.alt.si.value),
                # TODO srsly now? FFS
                'az_prediction': reconstruction.az.si.value,
                'core_x_prediction': reconstruction.core_x.si.value,
                'core_y_prediction': reconstruction.core_y.si.value,
                #'array_event_id': array_event_id,
                # 'h_max_prediction': reconstruction.h_max.si.value
                }

