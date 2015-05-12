(ns p-diffy.analyzer
  (:require [p-diffy.diff :as diff]
            [clojure.java.io]
            [hiccup.page])
  (:import [java.io File ByteArrayOutputStream]
           [javax.imageio ImageIO]
           [java.awt.image BufferedImage]))

;; path handling

(defn- folder-pairs
  [^File root-folder]
  (partition 2 1
             (remove
              (fn [f] (.contains (.getAbsolutePath f) ".DS_Store"))
              (seq (.listFiles root-folder)))))

(defn analyze-folders
  "Doesn't traverse sub-dirs."
  [^File from ^File to]
  [from
   to
   (for [f1 (rest (file-seq to))
         :when (.endsWith (.getName f1) ".png")]
     (let [bi1 (ImageIO/read f1)
           f2 (File. from (.getName f1))
           bi2 (if (.exists f2)
                 (ImageIO/read f2)
                 (BufferedImage. (.getWidth bi1)
                                 (.getHeight bi1)
                                 BufferedImage/TYPE_INT_RGB))]
       [f1 (diff/compare-images bi1 bi2)]))])

;;; /index.html           html
;;; /from/to/to-file-name buffered-image

;; comparisons always refers to a sequence of [from to [ImageComparisonResult]]
;; might want to pack FolderComparison in a record

(def ^:private style-css
  "
.title { float: left;
         width: 30%; }

.screenshots { float: right;
               width: 70%; }

h2 { font-family: Helvetica;
     color: #aaa;
     float: right;
     padding-right: 10px;}

img { border: 1px solid #aaa;
      margin: 2px; }
")

(defn- index-page
  [comparisons]
  (hiccup.page/html5
   ;; todo: ensure this file exists as part of generate-files
   (hiccup.page/include-css "style.css")
   (for [[from to rs] (reverse comparisons)]
     [:div
      {:class "comparison"}
      [:div
       {:class "title"}
       [:h2 (.getPath to)]]
      [:div
       {:class "screenshots"}
       (for [[r-file r-image-comparison-result]
             (sort-by (comp - :difference second) rs)]
         (let [filename (format "%s/%s.png" (.getPath from) (.getPath r-file))
               difference (-> r-image-comparison-result
                              :difference
                              (* 100)
                              int
                              str)]
           [:a
            {:href filename}
            [:img
             {:src filename
              :width "200px"
              :style "padding: 10px"
              :data-difference difference}]]))]])))

(defn generate-files
  [comparisons]
  (let [index-filename "static/index.html"
        css-filename "static/style.css"]
    (clojure.java.io/make-parents index-filename)
    (spit index-filename
          (index-page comparisons))
    (spit css-filename style-css)
    (doseq [[from to rs] comparisons]
      (doseq [[r-file r-image-comparison-result] rs]
        ;; TODO: this path generation should be split out and not depend
        ;; on `from` in the same manner
        (let [outfile (File. (format "static/%s/%s.png"
                                     (.getPath from)
                                     (.getPath r-file)))]
          (clojure.java.io/make-parents outfile)
          (ImageIO/write (:bufferedImage r-image-comparison-result)
                         "png"
                         outfile))))))

(defn- generate-comparisons
  [folder]
  (map
   (partial apply analyze-folders)
   (folder-pairs (File. folder))))

(defn -main
  [& args]
  (-> "uswitch"
      generate-comparisons
      generate-files))

(comment

  (def comparisons
    (generate-comparisons "uswitch"))

  (generate-files comparisons)

  (-main)

  )
