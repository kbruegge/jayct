import click
from tqdm import tqdm
from ctapipe.io import event_source
from ctapipe.calib import CameraCalibrator
import json
import gzip
from ctapipe.image import hillas_parameters, tailcuts_clean, HillasParameterizationError



names_to_id = {'LSTCam': 1, 'NectarCam': 2, 'FlashCam': 3, 'DigiCam': 4, 'CHEC': 5}
types_to_id = {'LST': 1, 'MST': 2, 'SST': 3}
allowed_cameras = ['LSTCam', 'NectarCam', 'DigiCam']


def fill_mc_dict(event):
    mc = {}
    mc['energy'] = event.mc.energy.to_value('TeV')
    mc['alt'] = event.mc.alt.to_value('rad')
    mc['az'] = event.mc.az.to_value('rad')
    mc['core_x'] = event.mc.core_x.to_value('m')
    mc['core_y'] = event.mc.core_y.to_value('m')
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
@click.argument('output_file', type=click.Path(exists=False))
@click.option('--limit', default=-1, help='number of events to convert from the file.'
                                           'If a negative value is given, the whole file'
                                           'will be read')
def main(input_files, output_file, limit):
    '''
    The INPUT_FILE argument specifies the path to a simtel file. This script reads the
    camera definitions from there and puts them into a json file
    specified by OUTPUT_FILE argument.
    '''

    data = []
    for input_file in input_files:
        print(f'processing file {input_file}')
        try:
            source = event_source(
                input_url=input_file,
                max_events=limit if limit > 0 else None
            )
        except (EOFError, StopIteration):
            print(f'Could not produce eventsource. File might be truncated? {input_file}')
            return None

        calibrator = CameraCalibrator()


        for id, event in tqdm(enumerate(source)):
            if len(valid_triggerd_cameras(event)) < 2:
                continue
            calibrator(event)
            c = {}
            # import IPython; IPython.embed()

            c['array'] = fill_array_dict(valid_triggerd_telescope_ids(event))
            c['event_id'] = id
            c['images'] = fill_images_dict(event)
            c['mc'] = fill_mc_dict(event)

            # r = calculate_hillas(event)
            # from IPython import embed; embed()

            data.append(c)
        


    with gzip.open(output_file, 'wt') as of:
        json.dump(data, of, indent=2)


def calculate_hillas(event):

    hillas_dict = {}
    for telescope_id, dl1 in event.dl1.tel.items():
        telescope = event.inst.subarray.tels[telescope_id]
        mask = tailcuts_clean(
            telescope.camera,
            dl1.image[0],
            boundary_thresh=4.5,
            picture_thresh=8,
            min_number_picture_neighbors=1,
        )


        if telescope.camera.cam_id not in allowed_cameras:
            continue

        cleaned = dl1.image[0].copy()
        cleaned[~mask] = 0

        try:
            hillas_dict[telescope_id] = hillas_parameters(telescope.camera, cleaned)
        except HillasParameterizationError:
            pass  # skip failed parameterization (normally no signal)

    return hillas_dict



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
