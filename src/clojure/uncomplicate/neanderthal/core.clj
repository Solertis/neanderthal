(ns ^{:author "Dragan Djuric"}
  uncomplicate.neanderthal.core
  "Contains type-agnostic linear algebraic functions. Typically,
  you would want to require this namespace regardless of the actual type
  (real, complex etc.) of the vectors and matrices that you use.
  Aditionally, you need to require a namespace specific to the
  type of primitive data you use (real, complex, etc.)

  You need to take care to only use vectors and matrices
  of the same type in the same function call. These functions do not support
  arguments of mixed real types. For example, you can not call the
  dot function with one double vector (dv) and one float vector (fv).

  ----- Example
  (ns test
    (:require [uncomplicate.neanderthal core real]))
  "
  (:require  [uncomplicate.neanderthal
              [protocols :as p]
              [math :refer [f= pow sqrt]]])
  (:import [uncomplicate.neanderthal.protocols Vector Matrix Block BLAS]))

(def  INCOMPATIBLE_BLOCKS_MSG
  "Operation is not permited on vectors with incompatible buffers,
  or dimensions that are incompatible in the context of the operation.
  1: %s
  2: %s")

(def ^:private INCOMPATIBLE_BLOCKS_MSG_3
  "Operation is not permited on vectors with incompatible buffers,
  or dimensions that are incompatible in the context of the operation.
  1: %s
  2: %s
  3: %s")

(def ^:private ROW_COL_MSG
  "Required %s %d is higher than the row count %d.")

(def ^:private DIMENSION_MSG
  "Incompatible dimension - expected:%d, actual:%d.")

(def ^:private STRIDE_MSG
  "Incompatible stride - expected:%d, actual:%d.")

;; ================== Category functions  ==============
(defn fmap!
  ([f x]
   (p/fmap! x f))
  ([f x y]
   (p/fmap! x f y))
  ([f x y z]
   (p/fmap! x f y z))
  ([f x y z w]
   (p/fmap! x f y z w))
  ([f x y z w & ws]
   (apply p/fmap! x f y z w ws)))

(defn fold
  ([x] ;;TODO all categorical functions than can be primitive should be checked
   (p/fold x))
  ([f x]
   (p/fold x f))
  ([f id x]
   (p/fold x f id)))

(defn freduce
  ([f x]
   (p/freduce x f))
  ([f acc x]
   (p/freduce x acc f))
  ([f acc x y]
   (p/freduce x acc f y))
  ([f acc x y z]
   (p/freduce x acc f y z))
  ([f acc x y z & ws]
   (apply p/freduce x acc f y z ws)))

;; ================= Group  ====================================================
(defn zero
  "Returns an empty instance of the same type and dimension(s)
  as x, which can be vector, matrix or any other neanderthal object."
  [x]
  (p/zero x))

;; ================= Vector ====================================================
(defn vect?
  "Returns true if x implements uncomplicate.neanderthal.protocols.Vector.

  (vect? (dv 1 2 3)) => true
  (vect? [1 2 3]) => false
  "
  [x]
  (instance? Vector x))

(defn matrix?
  "Returns true if x implements uncomplicate.neanderthal.protocols.Matrix.

  (matrix? (dge 3 2 [1 2 3 4 5 6])) => true
  (matrix? [[1 2] [3 4] [5 6]]) => false
  "
  [x]
  (instance? Matrix x))

(defn dim
  "Returns the dimension of a neanderthal vector x.
  (dim (dv 1 2 3) => 3)
  "
  ^long [^Vector x]
  (.dim x))

(defn ecount
  "Returns the total number of elements in all dimensions of a block
  of (possibly strided) memory.

  (ecount (dv 1 2 3) => 3)
  (ecount (dge 2 3) => 6)
  "
  ^long [^Block x]
  (.count x))

(defn subvector
  "Returns a subvector starting witk k, l entries long,
  which is a part of a neanderthal vector x.

  The resulting subvector has a live connection to the
  vector data. Any change to the subvector data will affect
  the vector data.
  If you wish to disconnect the subvector from the parent
  vector, make a copy of the subvector (copy subx) prior
  to any changing operation.

  (subvector (dv 1 2 3 4 5 6) 2 3)
  => #<DoubleBlockVector| n:3, stride:1 (3.0 4.0 5.0)>
  "
  [^Vector x ^long k ^long l]
  (.subvector x k l))

;; ================= Matrix =======================

(defn mrows
  "Returns the number of rows of the matrix m.
  (mrows (dge 3 2 [1 2 3 4 5 6])) => 3
  "
  ^long [^Matrix m]
  (.mrows m))

(defn ncols
  "Returns the number of columns of the matrix m.
  (mrows (dge 3 2 [1 2 3 4 5 6])) => 2
  "
  ^long [^Matrix m]
  (.ncols m))

(defn row
  "Returns the i-th row of the matrix m as a vector.

  The resulting vector has a live connection to the
  matrix data. Any change to the vector data will affect
  the matrix data.
  If you wish to disconnect the vector from the parent
  matrix, make a copy of the vector (copy x) prior
  to any changing operation.

  (row (dge 3 2 [1 2 3 4 5 6]) 1)
  => #<DoubleBlockVector| n:2, stride:3 (2.0 5.0)>
  "
  [^Matrix m ^long i]
  (if (< -1 i (.mrows m))
    (.row m i)
    (throw (IndexOutOfBoundsException.
            (format ROW_COL_MSG "row" i (.mrows m))))))

(defn col
  "Returns the i-th column of the matrix m as a vector.

  The resulting vector has a live connection to the
  matrix data. Any change to the vector data will affect
  the matrix data.
  If you wish to disconnect the vector from the parent
  matrix, make a copy of the vector (copy x) prior
  to any changing operation.

  (col (dge 3 2 [1 2 3 4 5 6]) 0)
  #<DoubleBlockVector| n:3, stride:1 (1.0 2.0 3.0)>
  "
  [^Matrix m ^long j]
  (if (< -1 j (.ncols m))
    (.col m j)
    (throw (IndexOutOfBoundsException.
            (format ROW_COL_MSG "col" j (.ncols m))))))

(defn cols
  "Returns a lazy sequence of vectors that represent
  columns of the matrix m.
  The vectors have a live connection to the matrix data.
  "
  [^Matrix m]
  (map #(.col m %) (range (.ncols m))))

(defn rows
  "Returns a lazy sequence of vectors that represent
  rows of the matrix m.
  The vectors have a live connection to the matrix data.
  "
  [^Matrix m]
  (map #(.row m %) (range (.mrows m))))

(defn submatrix
  "Returns a submatrix of m starting with row i, column j,
  that has k columns and l rows.

  The resulting submatrix has a live connection to the
  matrix m's data. Any change to the submatrix data will affect
  m's data.
  If you wish to disconnect the submatrix from the parent
  matrix, make a copy of the submatrix (copy subm) prior
  to any changing operation.

  (submatrix (dge 4 3 (range 12)) 1 1 2 1)
  => #<DoubleGeneralMatrix| COL, mxn: 2x1, ld:4 ((5.0 6.0))>
  "
  ([^Matrix a i j k l]
   (if (and (<= 0 (long i) (+ (long i) (long k)) (.mrows a))
            (<= 0 (long j) (+ (long j) (long l)) (.ncols a)))
     (.submatrix a i j k l)
     (throw (IndexOutOfBoundsException.
             (format "Submatrix %d,%d %d,%d is out of bounds of %dx%d."
                     i j k l (.mrows a) (.ncols a))))))
  ([^Matrix a k l]
   (submatrix a 0 0 k l)))

(defn trans
  "Transposes matrix m, i.e returns a matrix that has
  m's columns as rows.
  The resulting matrix has a live connection to m's data.

  (trans (dge 3 2 [1 2 3 4 5 6]))
  => #<DoubleGeneralMatrix| ROW, mxn: 2x3, ld:3 ((1.0 2.0 3.0) (4.0 5.0 6.0))>
  "
  [^Matrix m]
  (.transpose m))

;;================== BLAS 1 =======================

(defn dot
  "BLAS 1: Dot product.
  Computes the dot product of vectors x and y.

  (dot (dv 1 2 3) (dv 1 2 3)) => 14.0
  "
  [x y]
  (if (and (p/compatible x y) (= (dim x) (dim y)))
    (.dot (p/engine x) x y)
    (throw (IllegalArgumentException.
            (format INCOMPATIBLE_BLOCKS_MSG x y)))))

(defn nrm2
  "BLAS 1: Euclidean norm.
  Computes the Euclidan (L2) norm of vector x.

  (nrm2 (dv 1 2 3)) => 3.7416573867739413
  "
  [x]
  (.nrm2 (p/engine x) x))

(defn asum
  "BLAS 1: Sum absolute values.
  Sums absolute values of entries of vector x.

  (asum (dv -1 2 -3)) => 6.0
  "
  [x]
  (.asum (p/engine x) x))

(defn iamax
  "BLAS 1: Index of maximum absolute value.
  The index of a first entry that has the maximum value.
  (iamax (dv 1 3 2)) => 1
  "
  ^long [x]
  (.iamax (p/engine x) x))

(defn scal!
  "BLAS 1: Scale vector.

  Computes x = ax
  where:
  - x is a vector,
  - a is a scalar.

  Multiplies vector x by scalar alpha, i.e scales the vector.
  After scal!, x will be changed.

  (scal! 1.5  (dv 1 2 3))
  => #<DoubleBlockVector| n:3, stride:1, (1.5 3.0 4.5)>
  "
  [alpha x]
  (do
    (.scal (p/engine x) alpha x)
    x))

(defn rot!
  "BLAS 1: Apply plane rotation.
  "
  ([x y ^double c ^double s]
   (if (p/compatible x y)
     (if (and (<= -1.0 c 1.0) (<= -1.0 c 1.0)
              (f= 1.0 (+ (pow c 2) (pow s 2))))
       (do
         (.rot (p/engine x) x y c s)
         x)
       (throw (IllegalArgumentException.
               "c and s must be sin and cos.")))
     (throw (IllegalArgumentException.
             (format INCOMPATIBLE_BLOCKS_MSG x y)))))
  ([x y c]
   (rot! x y c (sqrt (- 1.0 (pow c 2))))))

(defn rotg!
  "BLAS 1: Generate plane rotation.

  # Description:

  Computes  the  elements  of  a Givens plane rotation matrix:
  .           __    __     __ __    __ __
  .           | c  s |     | a |    | r |
  .           |-s  c | *   | b | =  | 0 |
  .           --    --     -- --    -- --

  where  r = +- sqrt ( a**2 + b**2 )
  and    c**2 + s**2 = 1.

  a, b, c, s are the four entries in the vector x.

  This rotation can be used to selectively introduce zero elements
  into a matrix.

  # Arguments:

  x: contains a, b, c, and s arguments (see the description).
  .  Has to have exactly these four entries, and must have stride = 1.

  # Result:

  x with the elements changed to be r, z, c, s.
  For a detailed description see
  http://www.mathkeisan.com/usersguide/man/drotg.html
  "
  [x]
  (if (= 4 (dim x))
    (do
      (.rotg (p/engine x) x)
      x)
    (throw (IllegalArgumentException.
            (format DIMENSION_MSG 4 (dim x))))))

(def ^:private ROTMG_COND_MSG
  "Arguments p and args must be compatible.
  p must have dimension 5 and args must have dimension 4.
  p: %s;
  args: %s ")

(defn rotmg!
  "BLAS 1: Generate modified plane rotation.
  "
  [p args]
  (if (and (p/compatible p args) (= 5 (dim p)) (= 4 (dim args)))
    (do
      (.rotmg (p/engine p) p args)
      p)
    (throw (IllegalArgumentException.
            (format ROTMG_COND_MSG p args)))))

(def ^:private ROTM_COND_MSG
  "Arguments x and y must be compatible and have the same dimensions.
  argument p must have dimension 5.
  x: %s;
  y: %s;
  p: %s")

(defn rotm!
  "BLAS 1: Apply modified plane rotation.
  "
  [x y p]
  (if (and (p/compatible x y) (= (dim x) (dim y)) (= 5 (dim p)))
    (do
      (.rotm (p/engine x) x y p)
      x)
    (throw (IllegalArgumentException.
            (format ROTM_COND_MSG x y p)))))

(defn swp!
  "BLAS 1: Swap vectors.
  Swaps the entries of vectors x and y. x and y must have
  equal dimensions. Both x and y will be changed.
  Also works on matrices.

  (def x (dv 1 2 3))
  (def y (dv 3 4 5))
  (swp! x y)
  => #<DoubleBlockVector| n:3, stride:1 (3.0 4.0 5.0)>
  y
  => #<DoubleBlockVector| n:3, stride:1 (1.0 2.0 3.0)>
  "
  [x y]
  (if (p/compatible x y)
    (if (= (ecount x) (ecount y))
      (do
        (.swap (p/engine x) x y)
        x)
      (throw (IllegalArgumentException.
              (format DIMENSION_MSG (ecount x) (ecount y)))))
    (throw (IllegalArgumentException.
            (format INCOMPATIBLE_BLOCKS_MSG x y)))))

(defn copy!
  "BLAS 1: Copy vector.
  Copies the entries of x into y and returns x. x and y must have
  equal dimensions. y will be changed. Also works on
  matrices.

  (def x (dv 1 2 3))
  (def y (dv 3 4 5))
  (copy! x y)
  => #<DoubleBlockVector| n:3, stride:1 (1.0 2.0 3.0)>
  y
  => #<DoubleBlockVector| n:3, stride:1 (1.0 2.0 3.0)>
  "
  [x y]
  (if (p/compatible x y)
    (if (= (ecount x) (ecount y))
      (do
        (.copy (p/engine x) x y)
        y)
      (throw (IllegalArgumentException.
              (format DIMENSION_MSG (ecount x) (ecount y)))))
    (throw (IllegalArgumentException.
            (format INCOMPATIBLE_BLOCKS_MSG x y)))))

(defn copy
  "Returns a new vector and copies the entries from x.
  Changes to the resulting vectors do not affect x.

  (copy (dv 1 2 3))
  => #<DoubleBlockVector| n:3, stride:1 (1.0 2.0 3.0)>
  "
  [x]
  (let [y (p/zero x)]
    (copy! x y)
    y))

(defn axpy!
  "BLAS 1: Vector scale and add.

  Computes y = ax + y.
  where:
  x and y are vectors,
  a is a scalar.

  Multiplies vector x by scalar alpha and adds it to
  vector y. After axpy!, y will be changed.

  If called with 2 arguments, x and y, adds vector x
  to vector y.

  If called with more than 3 arguments, at least every
  other have to be a vector. A scalar multiplier may be
  included before each vector.

  If the dimensions of x and y are not compatible,
  throws IllegalArgumentException.

  (def x (dv 1 2 3))
  (def y (dv 2 3 4))
  (axpy! 1.5 x y)
  => #<DoubleBlockVector| n:3, stride:1, (3.5 6.0 8.5)>

  y => #<DoubleBlockVector| n:3, stride:1, (3.5 6.0 8.5)>

  (axpy! x y)
  => #<DoubleBlockVector| n:3, stride:1, (4.5 8.0 11.5)>

  (axpy! x y (dv 3 4 5) 2 (dv 1 2 3))
  => #<DoubleBlockVector| n:3, stride:1, (10.5 18.0 25.5)>
  "
  ([alpha x y]
   (if (and (p/compatible y x) (= (ecount x) (ecount y)))
     (do
       (.axpy (p/engine y) alpha x y)
       y)
     (throw (IllegalArgumentException.
             (format INCOMPATIBLE_BLOCKS_MSG x y)))))
  ([x y]
   (axpy! 1.0 x y))
  ([x y z & zs]
   (if (vect? x)
     (apply axpy! 1.0 x y z zs)
     (loop [res (axpy! x y z) s zs]
       (if-let [v (first s)]
         (let [r (rest s)]
           (if (vect? v)
             (recur (axpy! 1.0 v res) r)
             (recur (axpy! v (first r) res) (rest r))))
         res)))))

(defn axpy
  "A pure variant of axpy! that does not change
  any of the arguments. The result is a new vector instance.
  "
  ([x y]
   (axpy! 1.0 x (copy y)))
  ([alpha x y]
   (axpy! alpha x (copy y)))
  ([x y z w & ws]
   (if (number? x)
     (apply axpy! x y (copy z) w ws)
     (apply axpy! 1.0 x (copy y) z w ws))))

(defn ax
  "Multiplies vector x by a scalar a.
  Similar to scal!, but does not change x. The result
  is a new vector instance."
  [alpha x]
  (axpy! (p/zero x) alpha x))

(defn xpy
  "Sums vectors x, y, & zs. The result is a new vector instance."
  ([x y]
   (axpy! 1.0 x (copy y)))
  ([x y & zs]
   (loop [res (axpy! 1.0 x (copy y)) s zs]
     (if s
       (recur (axpy! 1.0 (first s) res) (next s))
       res))))

;;============================== BLAS 2 ========================================

(defn mv!
  "BLAS 2: Matrix-vector multiplication.

  Computes y = alpha a * x + beta * y
  where:
  a is a matrix,
  x and y are vectors,
  alpha and beta are scalars.

  Multiplies matrix a by scalar alpha and then multiplies
  the the resulting matrix by vector x. Adds the resulting
  vector to vector y previously scaled by scalar beta.
  Returns vector y, which contains the result and is changed by
  the operation.

  If alpha and beta are not provided, uses identity value as their values.

  If the dimensions of a, x and y are not compatible,
  throws IllegalArgumentException.

  (def a (dge 3 2 (range 6)))
  (def x (dv 1 2))
  (def y (dv 2 3 4))

  (mv! 2.0 a x 1.5 y)
  => #<DoubleBlockVector| n:3, stride:1, (15.0 22.5 30.0)>
  "
  ([alpha a x beta y]
   (if (and (p/compatible a x) (p/compatible a y)
            (= (ncols a) (dim x))
            (= (mrows a) (dim y)))
     (do (.mv (p/engine a) alpha a x beta y)
         y)
     (throw (IllegalArgumentException.
               (format INCOMPATIBLE_BLOCKS_MSG_3 a x y)))))
  ([alpha a x y]
     (mv! alpha a x 1.0 y))
  ([a x y]
     (mv! 1.0 a x 0.0 y)))

(defn mv
  "A pure version of mv! that returns the result
  in a new vector instance. Computes alpha a * x."
  ([alpha a x]
   (mv! alpha a x 0.0 (p/zero (col a 0))))
  ([a x]
   (mv 1.0 a x)))

(defn rank!
  "BLAS 2: General rank-1 update.

  Computes a = alpha * x * y' + a
  where:

  alpha is a scalar
  x and y are vectors, y' is a transposed vector,
  a is a mxn matrix.

  If called with 3 arguments, a, x, y, alpha is 1.0.

  (def a (dge 3 2 [1 1 1 1 1 1]))
  (rank! 1.5 (dv 1 2 3) (dv 4 5) a)
  => #<DoubleGeneralMatrix| COL, mxn: 3x2, ld:3, ((7.0 13.0 19.0) (8.5 16.0 23.5))>
  "
  ([alpha x y a]
   (if (and (p/compatible a x) (p/compatible a y)
            (= (mrows a) (dim x))
            (= (ncols a) (dim y)))
     (do
       (.rank (p/engine a) alpha x y a)
       a)
     (throw (IllegalArgumentException.
             (format INCOMPATIBLE_BLOCKS_MSG_3 a x y)))))
  ([x y a]
   (rank! 1.0 x y a)))

(defn rank
  "A pure version of rank! that returns the result
  in a new matrix instance.
  "
  ([alpha x y]
   (rank! (p/create-matrix x (dim x) (dim y)) alpha x y))
  ([x y]
   (rank 1.0 x y)))

;; =========================== BLAS 3 ==========================================

(defn mm!
  "BLAS 3: Matrix-matrix multiplication.

  Computes c = alpha a * b + beta * c
  where:
  a, b and c are matrices,
  alpha and beta are scalars.

  Multiplies matrix a by scalar alpha and then multiplies
  the resulting matrix by matrix b. Adds the resulting
  matrix to matrix c previously scaled by scalar beta.
  Returns matrix c, which contains the result and is changed by
  the operation.

  Can be called without scalars, with three matrix arguments.

  If the dimensions of a, b and c are not compatible,
  throws IllegalArgumentException.

  (def a (dge 2 3 (range 6)))
  (def a (dge 3 2 (range 2 8)))
  (def c (dge 2 2 [1 1 1 1]))

  (mm! c 1.5 a b 2.5)
  => #<DoubleGeneralMatrix| COL, mxn: 2x2, ld:2, ((35.5 49.0) (62.5 89.5))>

  (def c (dge 2 2))
  (mm! c a b)
  => #<DoubleGeneralMatrix| COL, mxn: 2x2, ld:2, ((22.0 31.0) (40.0 58.0))>
  "
  ([alpha a b beta c]
   (if (and (p/compatible c a) (p/compatible c b))
     (if (and (= (ncols a) (mrows b))
              (= (mrows a) (mrows c))
              (= (ncols b) (ncols c)))
       (do
         (.mm (p/engine c) alpha a b beta c)
         c)
       (throw (IllegalArgumentException.
               (format "Incompatible dimensions - a:%dx%d, b:%dx%d, c:%dx%d."
                       (mrows c) (ncols c)
                       (mrows a) (ncols a)
                       (mrows b) (ncols b)))))
     (throw (IllegalArgumentException.
             (format INCOMPATIBLE_BLOCKS_MSG_3 a b c)))))
  ([a b c]
   (mm! 1.0 a b 1.0 c))
  ([alpha a b c]
   (mm! alpha a b 1.0 c)))

(defn mm
  "A pure version of mm!, that returns the result
  in a new matrix instance.
  Computes alpha a * b"
  ([alpha a b]
   (mm! alpha a b 0.0 (p/create-matrix a (mrows a) (ncols b))))
  ([a b]
   (mm 1.0 a b)))
