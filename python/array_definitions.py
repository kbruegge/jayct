import click
from tqdm import tqdm
from ctapipe.io import event_source

import json


@click.command()
@click.argument('input_file', type=click.Path(exists=True))
@click.argument('output_file', type=click.Path(exists=False))
def main(input_file, output_file):
    '''
    The INPUT_FILE argument specifies the path to a simtel file. This script reads the
    array definition from there and puts them into a json file
    specified by OUTPUT_FILE argument.
    '''

    source = event_source(input_file)
    event = next(iter(source))
    instruments = event.inst

    # import IPython; IPython.embed()

    d = {}
    for tel_id, tel_description in tqdm(instruments.subarray.tel.items()):
        name = tel_description.camera.cam_id
        telescope_type = tel_description.type

        g = {}
        g['optical_focal_length'] = tel_description.optics.equivalent_focal_length.to_value('m')
        g['camera_name'] = name
        g['telescope_id'] = int(tel_id)
        g['telescope_type'] = telescope_type
        g['telescope_position_x'] = instruments.subarray.positions[tel_id][0].to_value('m')
        g['telescope_position_y'] = instruments.subarray.positions[tel_id][1].to_value('m')
        g['telescope_position_z'] = instruments.subarray.positions[tel_id][2].to_value('m')
        d[str(tel_id)] = g

    with open(output_file, 'w') as of:
        json.dump(d, of, indent=2)


if __name__ == '__main__':
    main()
