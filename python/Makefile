SIMTEL_FILE = ../../hb9/gamma_20deg_180deg_run1000___cta-prod3_desert-2150m-Paranal-merged.simtel.gz

all:
	python array_definitions.py $(SIMTEL_FILE) ../src/main/resources/array_definitions/cta_array_definition.json
	python camera_definitions.py $(SIMTEL_FILE) ../src/main/resources/camera_definitions/cta_camera_definition.json
	python convert_raw_data.py $(SIMTEL_FILE) ../src/main/resources/data/images.json.gz --limit 10