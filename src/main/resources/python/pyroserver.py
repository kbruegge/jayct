import Pyro4

from reconstruct_direction import Reconstructor

def main():
    Pyro4.Daemon.serveSimple(
            {
                Reconstructor: 'streams.processors'
            },
            ns=True
    )


if __name__ == '__main__':
    main()