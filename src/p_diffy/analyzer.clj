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

(defrecord FileComparisonResult [from-file to-file
                                 image-comparison-result])

(defrecord FolderComparison [from to file-comparison-results])

(defn analyze-folders
  "Doesn't traverse sub-dirs. Returns a FolderComparison."
  [^File from ^File to]
  (FolderComparison.
   from
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
       (FileComparisonResult. f2 f1 (diff/compare-images bi1 bi2))))))

;;; /index.html           html
;;; /from/to/to-file-name buffered-image

(def ^:private style-css
  "
.comparison { clear: both; }

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
   [:title "p-diffy"]
   ;; todo: ensure this file exists as part of generate-files
   (hiccup.page/include-css "style.css")
   (for [c (reverse comparisons)]
     [:div
      {:class "comparison"}
      [:div
       {:class "title"}
       [:h2 (.getPath (:to c))]]
      [:div
       {:class "screenshots"}
       (for [fcr (:file-comparison-results c)]
         (let [filename (format "%s/%s.png"
                                (.getPath (:from c))
                                (.getPath (:from-file fcr)))
               difference (-> (:image-comparison-result fcr)
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
    (doseq [c comparisons]
      (doseq [fcr (:file-comparison-results c)]
        ;; TODO: this path generation should be split out and not depend
        ;; on `from` in the same manner
        (let [outfile (File. (format "static/%s/%s.png"
                                     (.getPath (:from c))
                                     (.getPath (:from-file fcr))))]
          (clojure.java.io/make-parents outfile)
          (ImageIO/write (:bufferedImage (:image-comparison-result fcr))
                         "png"
                         outfile))))))

(defn generate-comparisons
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
