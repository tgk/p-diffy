(ns p-diffy.main
  (:require [p-diffy.analyzer]))

(defn -main
  [& args]
  (-> "uswitch"
      p-diffy.analyzer/generate-comparisons
      p-diffy.analyzer/generate-files))
