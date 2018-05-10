import click
import os
import pandas as pd
import fact.io

@click.command()
@click.argument('input_folder', type=click.Path(exists=True, dir_okay=True, file_okay=False))
@click.argument('output_file', type=click.Path(exists=False))
def main(input_folder, output_file):

    if os.path.exists(output_file):
        click.confirm(f'File {output_file} exists. Overwrite?', default=False, abort=True)
        os.remove(output_file)

    runs = pd.read_csv(os.path.join(input_folder, 'runs.csv'))
    array_events = pd.read_csv(os.path.join(input_folder, 'array_events.csv'))
    telescope_events = pd.read_csv(os.path.join(input_folder, 'telescope_events.csv'))

    fact.io.write_data(runs, output_file, key='runs', use_h5py=True)
    fact.io.write_data(array_events, output_file, key='array_events', mode='a', use_h5py=True)
    fact.io.write_data(telescope_events, output_file, key='telescope_events', mode='a', use_h5py=True)

if __name__ == '__main__':
    main()
