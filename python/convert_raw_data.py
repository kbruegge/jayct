import click
from tqdm import tqdm
from ctapipe.io.eventsourcefactory import EventSourceFactory
from ctapipe.calib import CameraCalibrator
import json
import gzip


names_to_id = {'LSTCam': 1, 'NectarCam': 2, 'FlashCam': 3, 'DigiCam': 4, 'CHEC': 5}
types_to_id = {'LST': 1, 'MST': 2, 'SST': 3}
allowed_cameras = ['LSTCam', 'NectarCam', 'DigiCam']


def fill_mc_dict(event):
    mc = {}
    mc['energy'] = event.mc.energy.value
    mc['alt'] = event.mc.alt.value
    mc['az'] = event.mc.az.value
    mc['core_x'] = event.mc.core_x.value
    mc['core_y'] = event.mc.core_y.value
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
@click.argument('input_file', type=click.Path(exists=True))
@click.argument('output_file', type=click.Path(exists=False))
@click.option('--limit', default=100, help='number of events to convert from the file.'
                                           'If a negative value is given, the whole file'
                                           'will be read')
def main(input_file, output_file, limit):
    '''
    The INPUT_FILE argument specifies the path to a simtel file. This script reads the
    camera definitions from there and puts them into a json file
    specified by OUTPUT_FILE argument.
    '''

    event_source = EventSourceFactory.produce(
        input_url=input_file,
        max_events=limit if limit > 1 else None,
    )

    calibrator = CameraCalibrator(
        eventsource=event_source,
    )
    data = []
    for id, event in tqdm(enumerate(event_source)):
        if len(valid_triggerd_cameras(event)) < 2:
            continue
        calibrator.calibrate(event)
        c = {}
        # import IPython; IPython.embed()

        c['array'] = fill_array_dict(valid_triggerd_telescope_ids(event))
        c['event_id'] = id
        c['images'] = fill_images_dict(event)
        c['mc'] = fill_mc_dict(event)


        data.append(c)


    with gzip.open(output_file, 'wt') as of:
        json.dump(data, of, indent=2)



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
