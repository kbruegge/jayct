all: result.pdf

./build/FINISHED_MEASUREMENT:
	mkdir -p ./build/
	for i in {1..24}; do \
		echo "Using $$i threads" ; \
		java -jar ../build/libs/jayct-0.1.0-SNAPSHOT-all.jar -l 360 -p 1 -s 1 -m $$i -w $$i ../src/main/resources/images.json.gz ../src/main/resources/classifier.json ./build/performance_$$i.csv; \
   	done
	touch ./build/FINISHED_MEASUREMENT


result.pdf: ./build/FINISHED_MEASUREMENT
    mkdir -p ./build/plots/
	python3 plot_csv_results.py ./build/ -o ./build/plots/result.pdf
