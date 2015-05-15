(ns p-diffy.main
  (:require [p-diffy.analyzer]
            [clojure.tools.cli :as cli]))

(def ^:private cli-options
  [["-i" "--in FOLDER" "Input folder"
    :default "/tmp/in/"]
   ["-o" "--out FOLDER" "Output folder"
    :default "static"]])

(defn -main
  [& args]
  (let [opts (cli/parse-opts args cli-options)
        in (-> opts :options :in)
        out (-> opts :options :out)]
    (-> in
        p-diffy.analyzer/generate-comparisons
        (p-diffy.analyzer/generate-files out))))
