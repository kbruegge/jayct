SIMTEL_FILE = ../../cta_rta_analysis/data/raw/gamma_20deg_0deg_run6911___cta-prod3-merged_desert-2150m-Paranal-3HB89-NGFD.simtel.gz

all:
	python array_definitions.py $(SIMTEL_FILE) ../src/main/resources/array_definitions/cta_array_definition.json
	python camera_definitions.py $(SIMTEL_FILE) ../src/main/resources/camera_definitions/cta_camera_definition.json
