package generators.graphics.HelpersOptischeTriangulation;

import algoanim.util.Coordinates;

public class Vector {

    private double x;
    private double y;

    public static final Vector X_AXIS;
    public static final Vector Y_AXIS;
    public static final Vector NEGATIVE_X_AXIS;
    public static final Vector NEGATIVE_Y_AXIS;

    public static final double EPSILON = 0.00001;

    // static constructor
    static {
        X_AXIS = new Vector(1, 0);
        Y_AXIS = new Vector(0, 1);
        NEGATIVE_X_AXIS = new Vector(-1, 0);
        NEGATIVE_Y_AXIS = new Vector(0, -1);
    }

    /**
     * Constructor of the vector class
     * @param x x coordinate of the vector
     * @param y y coordinate of the vector
     */
    public Vector(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Constructor of the vector class for two points
     * @param x1 x coordinate of the first point
     * @param y1 y coordinate of the first point
     * @param x2 x coordinate of the second point
     * @param y2 y coordinate of the second point
     */
    public Vector(double x1, double y1, double x2, double y2) {
        this.x = x2 - x1;
        this.y = y2 - y1;
    }

    /**
     * Calculates the vector from two given vertices
     * @param p1 the first point
     * @param p2 the second point
     */
    public Vector(Vector p1, Vector p2) {
        Vector v = p2.subtract(p1);
        this.x = v.getX();
        this.y = v.getY();
    }

    /**
     * https://stackoverflow.com/a/1088058
     * Checks if the current direction vector collides with the given circle
     * @param start the point where the direction vector should start
     * @param circlePos the position of the circle
     * @param radius the radius of the circle
     * @return returns the position of the detected collisions or null if there is no collision
     */
    public Vector[] checkCollisionWithCircle(Vector start, Vector circlePos, double radius) {
        Vector dirNormalized = this.normalize(); // normalize direction vector
        // calculate distance between start and E, where E is the point of start-end closest to the circle center
        double t = dirNormalized.getX() * (circlePos.getX() - start.getX()) + dirNormalized.getY() * (circlePos.getY() - start.getY());
        // compute vector from point E
        Vector e = new Vector(t * dirNormalized.getX() + start.getX(), t * dirNormalized.getY() + start.getY());
        // compute distance between E and circle middle point
        double lec = Math.sqrt(Math.pow(e.getX() - circlePos.getX(), 2) + Math.pow(e.getY() - circlePos.getY(), 2));
        // check if the line intersects with the circle
        if(lec < radius) {
            // calculate distance from t to circle intersection point
            double dt = Math.sqrt(radius * radius - lec * lec);
            // calculate the first collision point
            Vector f = new Vector((t - dt) * dirNormalized.getX() + start.getX(), (t - dt) * dirNormalized.getY() + start.getY());
            // calculate the second collision point
            Vector g = new Vector((t + dt) * dirNormalized.getX() + start.getX(), (t + dt) * dirNormalized.getY() + start.getY());
            return new Vector[] { f, g };
        } else if(lec == radius) { // line is tangent to the circle
            return new Vector[] { e };
        } else { // no collision with the cirle
            return null; 
        }
    } 

    /**
     * Adds a vector to the current vector
     * @param v the vector to add
     * @return returns a new vector with the sum of the two vectors
     */
    public Vector add(Vector v) {
        if(v == null) throw new IllegalArgumentException();
        return new Vector(this.x + v.getX(), this.y + v.getY());
    }

    /**
     * Subtracts a vector from the current vector
     * @param v the vector to subtract
     * @return returns an new vector with the difference of the two vectors
     */
    public Vector subtract(Vector v) {
        if(v == null) throw new IllegalArgumentException();
        return new Vector(this.x - v.getX(), this.y - v.getY());
    }

    /**
     * Offsets the current Vector
     * @param offsetX the x offset
     * @param offsetY the y offset
     * @return returns a new vector which has the given offset
     */
    public Vector offset(double offsetX, double offsetY){
        return new Vector(this.x + offsetX, this.y + offsetY);
    }

    /**
     * Multiplies the current vector with a value
     * @param d the value to multiply with
     * @return returns a new vector which is multiplied
     */
    public Vector multiply(double d){
        return new Vector(this.x * d, this.y * d);
    }

    /**
     * Divides the current vector with a value
     * @param d the value to divide with
     * @return returns a new vector which is divided
     */
    public Vector divide(double d) {
        if(d == 0) throw new IllegalArgumentException();
        return new Vector(this.x / d, this.y / d);
    }

    /**
     * Returns the opposite vector of the current vector
     * @return the opposite vector
     */
    public Vector opposite() {
        return new Vector(-this.x, -this.y);
    }

    /**
     * Normalizes the current vector
     * @return returns the normalized vector
     */
    public Vector normalize() {
        return divide(this.magnitude());
    }

    /**
     * Calculates the angle between the current vector and a given vector
     * @param v the second vector
     * @return returns the angle between the current vector and the given vector in radians(!)
     */
    public double calculateAngle(Vector v) {
        if(v == null) throw new IllegalArgumentException();
        double division = this.dotProduct(v) / (this.magnitude() * v.magnitude());
        return Math.acos(division);
    }

    /**
     * Calculates the magnitude of the vector
     * @return returns the magnitude of the given vector
     */
    public double magnitude() {
        return Math.sqrt(this.x * this.x + this.y * this.y);
    }

    /**
     * Calculates the dot product between two vectors
     * @param v the other vector
     * @return the dot product of current vector and the given vector
     */
    public double dotProduct(Vector v) {
        if(v == null) throw new IllegalArgumentException();
        return this.x * v.getX() + this.y * v.getY();
    }

    /**
     * Rotates a vector around a vector
     * @param v the vector where the current vector needs to be rotated around
     * @param angle the angle of the rotation in radians(!)
     * @return returns the rotated vector
     */
    public Vector rotateAroundPoint(Vector v, double angle) {
        double x = Math.cos(angle) * (this.getX() - v.getX()) - Math.sin(angle) * (this.y - v.getY()) + v.getX();
        double y = Math.sin(angle) * (this.getX() - v.getX()) + Math.cos(angle) * (this.y - v.getY()) + v.getY();
        return new Vector(x, y);
    }
    
    /**
     * Getter for x coordinate
     * @return x coordinate of the vector
     */
    public double getX() {
        return x;
    }

    /**
     * Getter for y coordinate
     * @return y coordinate of the vector
     */
    public double getY() {
        return y;
    }

    /**
     * Sets the x coordinate of the vector
     * @param x the new coordinate value
     */
    public void setX(double x) {
        this.x = x;
    }

    /**
     * Sets the y coordinate of the vector
     * @param y the new coordinate value
     */
    public void setY(double y) {
        this.y = y;
    }

    /**
     * Returns the current vector as a coordinates object
     * @return the coordinate object
     */
    public Coordinates toCoordinates() {
        return new Coordinates((int) this.x, (int) this.y);
    }

    @Override
    public String toString() {
        return "(" + this.getX() + ", " + this.getY() + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null || !(obj instanceof Vector)) return false;
        Vector v = (Vector) obj;
        return this.x + EPSILON > v.getX() && this.x - EPSILON < v.getX() && this.y + EPSILON > v.getY() && this.y - EPSILON < v.getY();
    }
}