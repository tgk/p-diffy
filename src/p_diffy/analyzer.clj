(ns p-diffy.analyzer
  (:require [p-diffy.diff :as diff]
            [org.httpkit.server :as http-server]
            [clojure.java.io]
            [hiccup.page])
  (:import [java.io File ByteArrayOutputStream]
           [javax.imageio ImageIO]
           [java.awt.image BufferedImage]))

;; path handling

(defn- folder-pairs
  [^File root-folder]
  (partition 2 1 (seq (.listFiles root-folder))))

(defn analyze-folders
  "Doesn't traverse sub-dirs."
  [^File from ^File to]
  [from
   to
   (for [f1 (rest (file-seq to))]
     (let [bi1 (ImageIO/read f1)
           f2 (File. from (.getName f1))
           bi2 (if (.exists f2)
                 (ImageIO/read f2)
                 (BufferedImage. 0 0 BufferedImage/TYPE_INT_RGB))]
       [f1 (diff/compare-images bi1 bi2)]))])

(comment

  (apply analyze-folders
         (first (folder-pairs (File. "example"))))

  )

;; Ring path - could (should) be replaced by generating static pages

;;; /                     html
;;; /from/to/to-file-name buffered-image

;; comparisons always refers to a sequence of [from to [ImageComparisonResult]]
;; might want to pack FolderComparison in a record

(defn- buffered-image-route-map
  "Goes through the comparisons results and returns a map from routes to
  image filenames."
  [comparisons]
  (apply
   merge
   (for [[from to rs] comparisons]
     (into
      {}
      (for [[r-file r-image-comparison-result] rs]
        ;; TODO: this path generation should be split out and not depend
        ;; on `from` in the same manner
        [(format "/%s/%s" (.getPath from) (.getPath r-file))
         (:bufferedImage r-image-comparison-result)])))))

(defn- img-response
  [^BufferedImage bi]
  (let [out-stream (ByteArrayOutputStream.)]
    (ImageIO/write bi "png" out-stream)
    {:status 200
     :body   (clojure.java.io/input-stream (.toByteArray out-stream))}))

(defn- index-page
  [comparisons]
  (hiccup.page/html5
   (for [[from to rs] (reverse comparisons)]
     [:div
      (cons
       [:h2 (.getPath to)]
       (for [[r-file r-image-comparison-result]
             (sort-by (comp - :difference second) rs)]
         (let [filename (format "/%s/%s" (.getPath from) (.getPath r-file))
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
              :data-difference difference}]])))]
     )))

(defn router
  [comparisons]
  (let [img-route-map (buffered-image-route-map comparisons)]
    (fn [req]
      (if-let [bi (get img-route-map (:uri req))]
        (img-response bi)
        {:status 200
         :body (index-page comparisons)}))))

(comment

  (def comparisons
    (map
     (partial apply analyze-folders)
     (folder-pairs (File. "uswitch"))))

  (def r (router comparisons))

  (def server (http-server/run-server #'r {:port 8080}))
  (server)

  (first comparisons)

  (keys (buffered-image-route-map comparisons))

  )
