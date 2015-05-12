(ns p-diffy.diff
  (:import [javax.imageio ImageIO]
           [java.io File]
           [java.awt.image BufferedImage]
           [java.awt Color]))

(defrecord ImageComparisonResult [difference bufferedImage])

(defn compare-images
  "Compares BufferdImage i1 and i2 and returns an
  ImageComparisonResult."
  [^BufferedImage i1 ^BufferedImage i2]
  (let [width  (max (.getWidth  i1) (.getWidth  i2))
        height (max (.getHeight i1) (.getHeight i2))
        difference (atom 0) ; good enough for a first pass ;-)
        i3 (BufferedImage.
            width
            height
            BufferedImage/TYPE_INT_RGB)]
    (doseq [x (range width), y (range height)]
      (if (and (< x (.getWidth  i1)) (< x (.getWidth  i2))
               (< y (.getHeight i1)) (< y (.getHeight i2))
               (= (.getRGB i1 x y) (.getRGB i2 x y)))
        (.setRGB i3 x y (.getRGB i1 x y))
        (do (.setRGB i3 x y (.getRGB Color/RED))
            (swap! difference inc))))
    (ImageComparisonResult. (/ @difference (* width height)) i3)))

(comment

  (let [f (File. "difference.png")
        i1 (ImageIO/read (File. "example/2015-05-11/01.front.png"))
        i2 (ImageIO/read (File. "example/2015-05-13/01.front.png"))
        result (compare-images i1 i2)]
    (ImageIO/write (:bufferedImage result) "png" f)
    (float (:difference result)))

)
