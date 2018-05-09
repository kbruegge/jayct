import requests
from requests.auth import HTTPBasicAuth
import click
import shutil
import os
import re
from tqdm import tqdm



BASEURL = 'https://www.mpi-hd.mpg.de/personalhomes/bernlohr/cta-raw/Prod-3/Paranal-3HB89/'


def download_file(filename, password):
    url = f'{BASEURL}{filename}'

    r = requests.get(url, auth=HTTPBasicAuth('CTA', password), stream=True)

    if r.status_code != 200:
        print(f'File {filename} returned status code {r.status_code}')
        r.close()
        return

    # print(f'Beginning file download of {filename}')
    with open(filename, 'wb') as f:
        shutil.copyfileobj(r.raw, f)
    # print('Done')

    r.close()


def get_links(type, password):

    url = BASEURL
    print('contacting server')
    r = requests.get(url, auth=('CTA', password))

    print('Status code: {}'.format(r.status_code))

    if type == 'gamma_diffuse':
        type = 'gamma'
        regex = r"href=\"({}_20deg_0deg.+?NGFD_cone10.simtel.gz)".format(type)
    else:
        regex = r"href=\"({}_20deg_0deg.+?NGFD.simtel.gz)".format(type)

    links = re.findall(regex, r.text)
    return links


@click.command()
@click.argument('output_file', type=click.Path(exists=False))
@click.option('-p', '--password')
@click.option('-t', '--type', type=click.Choice(['gamma', 'proton', 'gamma_diffuse']), default='proton')
def main(output_file, password, type):

    if os.path.exists(output_file):
        click.confirm(f'File {output_file} exists. Overwrite?', default=False, abort=True)
        os.remove(output_file)

    links = get_links(type, password)
    print(f'Found {len(links)} links')

    for filename in tqdm(links):
        download_file(filename, password)


if __name__ == '__main__':
    main()
