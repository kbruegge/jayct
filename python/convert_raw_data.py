import click
from tqdm import tqdm
from ctapipe.io.eventsourcefactory import EventSourceFactory
from ctapipe.calib import CameraCalibrator
import json
import gzip
import pyhessio
import numpy as np
import os


names_to_id = {'LSTCam': 1, 'NectarCam': 2, 'FlashCam': 3, 'DigiCam': 4, 'CHEC': 5}
types_to_id = {'LST': 1, 'MST': 2, 'SST': 3}
allowed_cameras = ['LSTCam', 'NectarCam', 'DigiCam']

class NumpyEncoder(json.JSONEncoder):
    def default(self, obj):
        if isinstance(obj, np.integer):
            return int(obj)
        elif isinstance(obj, np.floating):
            return float(obj)
        elif isinstance(obj, np.ndarray):
            return obj.tolist()
        else:
            return super(MyEncoder, self).default(obj)


def read_simtel_mc_information(simtel_file):
    with pyhessio.open_hessio(simtel_file) as f:
        # do some weird hessio fuckup
        eventstream = f.move_to_next_event()
        _ = next(eventstream)

        d = {
            'mc_spectral_index': f.get_spectral_index(),
            'mc_num_reuse': f.get_mc_num_use(),
            'mc_num_showers': f.get_mc_num_showers(),
            'mc_max_energy': f.get_mc_E_range_Max(),
            'mc_min_energy': f.get_mc_E_range_Min(),
            'mc_max_scatter_range': f.get_mc_core_range_Y(),  # range_X is always 0 in simtel files
            'mc_max_viewcone_radius': np.deg2rad(f.get_mc_viewcone_Max()),
            'mc_min_viewcone_radius': np.deg2rad(f.get_mc_viewcone_Min()),
            'run_id': f.get_run_number(),
            'mc_max_altitude': f.get_mc_alt_range_Max(),
            'mc_max_azimuth': f.get_mc_az_range_Max(),
            'mc_min_altitude': f.get_mc_alt_range_Min(),
            'mc_min_azimuth': f.get_mc_az_range_Min(),
        }

        return d


def fill_mc_dict(event, run_information):
    mc = {
    'mc_alt': event.mc.alt.to('rad').value,
    'mc_az': event.mc.az.to('rad').value,
    'mc_core_x': event.mc.core_x.value,
    'mc_core_y': event.mc.core_y.value,
    'num_triggered_telescopes': len(valid_triggerd_cameras(event)),
    'mc_height_first_interaction': event.mc.h_first_int.value,
    'mc_energy': event.mc.energy.to('TeV').value,
    'mc_corsika_primary_id': event.mc.shower_primary_id,
    'run_id': event.r0.obs_id,
    'array_event_id': event.dl0.event_id,
    }
    mc.update(run_information)

    return mc


def fill_array_dict(tels):
    d = {}
    d['triggered_telescopes'] = tels
    d['num_triggered_telescopes'] = len(tels)
    return d


def fill_images_dict(event):
    img_dict = {}
    for telescope_id, dl1 in event.dl1.tel.items():
        camera = event.inst.subarray.tels[telescope_id].camera
        if camera.cam_id not in allowed_cameras:
            continue
        image = dl1.image[0]
        img_dict[str(telescope_id)] = image.tolist()

    return img_dict



@click.command()
@click.argument('input_files', nargs=-1, type=click.Path(exists=True))
@click.argument('output_dir', type=click.Path(exists=False))
@click.option('--limit', default=-1, help='number of events to convert from the file.'
                                           'If a negative value is given, the whole file'
                                           'will be read')
def main(input_files, output_dir, limit):
    '''
    The INPUT_FILE argument specifies the path to a simtel file. This script reads the
    camera definitions from there and puts them into a json file
    specified by OUTPUT_FILE argument.
    '''


    for input_file in tqdm(input_files):
        data = []
        run_information = read_simtel_mc_information(input_file)

        event_source = EventSourceFactory.produce(
            input_url=input_file,
            max_events=limit if limit > 1 else None,
        )

        calibrator = CameraCalibrator(
            eventsource=event_source,
        )

        for id, event in tqdm(enumerate(event_source)):
            if len(valid_triggerd_cameras(event)) < 2:
                continue
            calibrator.calibrate(event)
            c = {}

            c['array'] = fill_array_dict(valid_triggerd_telescope_ids(event))
            c['event_id'] = event.dl0.event_id
            c['run_id'] =  event.r0.obs_id
            c['images'] = fill_images_dict(event)
            c['mc'] = fill_mc_dict(event, run_information)


            data.append(c)

        name = os.path.basename(input_file)
        name, _ = os.path.splitext(name)
        output_name = os.path.join(output_dir, name + '.json.gz')
        print(f'Writing to file: {output_name}')
        with gzip.open(output_name, 'wt') as of:
            json.dump(data, of, indent=2, cls=NumpyEncoder)



def valid_triggerd_cameras(event):
    triggerd_tel_ids = event.trig.tels_with_trigger
    triggerd_camera_names = [event.inst.subarray.tels[i].camera.cam_id for i in triggerd_tel_ids]
    valid_triggered_cameras = list(filter(lambda c: c in allowed_cameras, triggerd_camera_names))
    return valid_triggered_cameras


def valid_triggerd_telescope_ids(event):
    triggerd_tel_ids = event.trig.tels_with_trigger
    ids = []
    for id in triggerd_tel_ids:
        if event.inst.subarray.tels[id].camera.cam_id in allowed_cameras:
            ids.append(int(id))

    return ids


if __name__ == '__main__':
    main()
