package generators.graphics;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Locale;

import algoanim.animalscript.AnimalScript;
import algoanim.primitives.Arc;
import algoanim.primitives.Polyline;
import algoanim.primitives.Rect;
import algoanim.primitives.SourceCode;
import algoanim.primitives.Text;
import algoanim.primitives.generators.Language;
import algoanim.properties.AnimationPropertiesKeys;
import algoanim.properties.ArcProperties;
import algoanim.properties.CircleProperties;
import algoanim.properties.MatrixProperties;
import algoanim.properties.PointProperties;
import algoanim.properties.PolylineProperties;
import algoanim.properties.RectProperties;
import algoanim.properties.SourceCodeProperties;
import algoanim.properties.TextProperties;
import algoanim.util.Coordinates;
import algoanim.util.Offset;
import generators.framework.Generator;
import generators.framework.GeneratorType;
import generators.framework.ValidatingGenerator;
import generators.framework.properties.AnimationPropertiesContainer;
import generators.graphics.HelpersOptischeTriangulation.Vector;

/**
 * @author Egemen Ulutürk <egemen.ulutuerk@stud.tu-darmstadt.de> und Pascal Schikora <pascal.schikora@stud.tu-darmstadt.de>
 * @version 1.0
 */
public class OptischeTriangulation implements ValidatingGenerator {

    /**
     * The concrete language object used for creating output
     */
    private Language lang;
    /**
     * Vector of the object
     */
    private Vector objPos;
    /**
     * Vector of the laser
     */
    private Vector laserPos;
    /**
     * Distance between Laser and lense (hole)
    */
    private double b;
    /**
     * Distance between camera-chip and lense (hole)
     */
    private double pictureHoleDistance;
    /**
     * Size of the camera chip
     */
    private double pictureChipSize;
    /**
     * The Chip-resolution
     */
    private int chipResolution;
    /**
     * The angle of the mirror in front of the laser
     */
    private double mirrorAngle;
    /**
     * step resolution of the mirror
     */
    private double mirrorStepResolution;
    /**
     * Radius of the circle
     */
    private double radius;
    /**
     * Properties of the SourceCode
     */
    private SourceCodeProperties sourceCodeProps;
    /**
     * Properties of text elements
     */
    private TextProperties textProps;
    /**
     * Properties of the angle lines
     */
    private PolylineProperties angleLineProps;
    /**
     * Properties of the angles
     */
    private ArcProperties arcProps;
    /**
     * Properties of the laser lines
     */
    private PolylineProperties laserLineProps;
    /**
     * Properties of the camera
     */
    private PolylineProperties cameraProps;
    /**
     * Properties of the circle objects
     */
    private CircleProperties circleProps;
    /**
     * The title of the animation
     */
    private Text header;
    /**
     * The rectangle around the animation title
     */
    private Rect headerRect;
    /**
     * static laser between laser-projector and mirror
     */
    private Polyline staticLaser;
    /**
     * actual and calculated depth values (alternating)
     */
    private ArrayList<Vector> table;
    /**
     * laser line from mirror to object
     */
    private Polyline lastLaser1;
    /**
     * laser line from object to camera
     */
    private Polyline lastLaser2;
    /**
     * the offset from the canvas
     */
    private final int OFFSET = 250; 
    /**
     * size added to get the actual camera size
     */
    private final int OVERSIZE = 2;
    /**
     * size of the Camera hole
     */
    private final int HOLESIZE = OVERSIZE + 2;
    /**
     * offset between laser and mirror
     */
    private final int MIRROR_LASER_OFFSET = 10;
    /**
     * height of the laser
     */
    private final int LASER_HEIGHT = 50;
    /**
     * width of the laser
     */
    private final int LASER_WIDTH = 10 / 2;
    /**
     * short description of the algorithm
     */
    private static final String DESCRIPTION =
        "Die optische Triangulation wird verwendet um die Tiefe eines Objektes zu bestimmen."
        +"\n"
        +"Hierfür wird ein Laser, ein Spiegel und eine Kamera benötigt."
        +"\n"
        +"Durch Anpassung des Spiegelwinkels lassen sich verschiedene Stellen eines Objekts"
        +"\n"
        +"abtasten und so seine gesamte Tiefe bestimmen.";
    /**
    * pseudo code of the algorithm
    */
    private static final String PSEUDO_CODE = 
        "1. Richte Spiegel aus (addiere Spiegelwinkel-Schrittweite auf vorherigen Winkel)"
        + "\n"
        + "2. Schieße Laser, falls Objekt nicht getroffen springe zu 1."
        + "\n"
        + "3. Schieße Laser, falls das Objekt getroffen wurde: "
        + "\n"
        + "3* Bestimme Pixel, auf dem der Laser landet"
        + "\n"
        + "3* Bestimme durch Spiegelwinkel und Laser-Chip-Schnittpunkt den abgetasteten Punkt";
    /**
     * The properties of the camera and laser outline
     */
    private RectProperties laserProps;
    /**
     * The source code for the animation
     */
    private SourceCode sourceCode;

    /**
     * Default constructor
     *
     * @param lang
     *          the conrete language object used for creating output
     */
    public OptischeTriangulation(Language lang) {
        super();
        this.lang = lang;
    }

    public OptischeTriangulation() {
        super();
    }

    /**
     * Initialisiert die Generierung
     */
    @Override
    public void init(){
        // Store the new language object
        this.lang = new AnimalScript(getAlgorithmName(), getAnimationAuthor(), 1500, 1500);
        // This initializes the step mode. Each pair of subsequent steps has to
        // be divdided by a call of lang.nextStep();
        this.lang.setStepMode(true);
        this.mirrorAngle = 90;
        this.lastLaser1 = null;
        this.lastLaser2 = null;
        this.table = new ArrayList<>();
    }

    /**
     * Validiert den Input
     */
    @Override
    public boolean validateInput(AnimationPropertiesContainer animationPropertiesContainer, Hashtable<String, Object> primitives) throws IllegalArgumentException {
        // load inputs
        double b = (double) primitives.get("basislaenge");
        double laserPosX = (Double) primitives.get("laserPosX");
        double laserPosY = (Double) primitives.get("laserPosY");
        double pictureChipSize = (double) primitives.get("c1");
        double pictureHoleDistance = (double) primitives.get("c2");
        double chipResolution = (Integer) primitives.get("anzahlPixel");
        double objPosX = (Double) primitives.get("kreisPosX");
        double objPosY = (Double) primitives.get("kreisPosY");
        double radius = (double) primitives.get("kreisRadius");
        double mirrorStepResolution = (double) primitives.get("spiegelwinkelSchrittweite");

        // check for invalid inputs
        if(b < 0) throw new IllegalArgumentException("Die Basislänge b darf nicht kleiner als Null sein!");
        if(laserPosX < 0 || laserPosY < 0) throw new IllegalArgumentException("Laserposition darf nicht negativ sein!");
        if(laserPosX > 1500 || laserPosY > 1500) throw new IllegalArgumentException("Die Laserposition darf 1500 nicht überschreiten!");
        if(pictureChipSize < 0) throw new IllegalArgumentException("Der Abstand zwischen Kameraloch und Chip darf nicht negativ sein!");
        if(pictureHoleDistance < 0) throw new IllegalArgumentException("Die Größe des Chips darf nicht negativ sein!");
        if(chipResolution <= 0) throw new IllegalArgumentException("Die Anzahl der Pixel darf nicht kleiner gleich 0 sein!");
        if(objPosX < 0 || objPosY < 0) throw new IllegalArgumentException("Die Objektposition darf nicht negativ sein!");
        if(objPosX > 1500 || objPosY > 1500) throw new IllegalArgumentException("Die Objektposition darf 1500 nicht überschreiten!");
        if(radius <= 0) throw new IllegalArgumentException("Der Radius des Objekts darf nicht kleiner gleich 0 sein!");
        if(mirrorStepResolution <= 0) throw new IllegalArgumentException("Die Schrittweite des Spiegelwinkels darf nicht negativ sein!");
        if(objPosX <= laserPosX) throw new IllegalArgumentException("Das Objekt darf nicht hinter der Kamera stehen!");
        if(objPosY <= laserPosY) throw new IllegalArgumentException("Das Objekt darf sich nicht über dem Laser befinden!");
        if(objPosX - radius <= laserPosX) throw new IllegalArgumentException("Das Objekt blockiert den Laser/die Kamera! (Positionen und Radius sind unzulässig!)");

        // valid input
        return true; 
    }

    /**
     *  Generiert die Ausgabe des Algorithmus
     */
    @Override
    public String generate(AnimationPropertiesContainer props,Hashtable<String, Object> primitives) {
        // load primitives
        this.b = (double) primitives.get("basislaenge");
        double laserPosX = (Double) primitives.get("laserPosX");
        double laserPosY = (Double) primitives.get("laserPosY");
        this.laserPos = new Vector(laserPosX, laserPosY);
        this.pictureChipSize = (double) primitives.get("c1");
        this.pictureHoleDistance = (double) primitives.get("c2");
        this.chipResolution = (Integer) primitives.get("anzahlPixel");
        double objPosX = (Double) primitives.get("kreisPosX");
        double objPosY = (Double) primitives.get("kreisPosY");
        this.objPos = new Vector(objPosX, objPosY);
        this.radius = (double) primitives.get("kreisRadius");
        this.mirrorStepResolution = (double) primitives.get("spiegelwinkelSchrittweite");

        // load properties
        this.sourceCodeProps = (SourceCodeProperties) props.getPropertiesByName("sourceCodeProps");
        this.textProps = (TextProperties) props.getPropertiesByName("textProps");
        this.angleLineProps = (PolylineProperties) props.getPropertiesByName("angleLineProps");
        this.laserLineProps = (PolylineProperties) props.getPropertiesByName("laserLineProps");
        this.circleProps = (CircleProperties) props.getPropertiesByName("kreisProps");
        this.arcProps = (ArcProperties) props.getPropertiesByName("arcProps");
        this.laserProps = (RectProperties) props.getPropertiesByName("laserProps");
        this.cameraProps = (PolylineProperties) props.getPropertiesByName("cameraProps");

        // create title
        TextProperties headerProps = new TextProperties();
        headerProps.set(AnimationPropertiesKeys.FONT_PROPERTY, new Font(Font.SANS_SERIF, Font.BOLD, 24));
        this.header = lang.newText(new Coordinates(20, 30), "Optische Triangulation", "header", null, headerProps);

        // create box around title
        RectProperties rectProps = new RectProperties();
        rectProps.set(AnimationPropertiesKeys.FILLED_PROPERTY, true);
        rectProps.set(AnimationPropertiesKeys.FILL_PROPERTY, Color.WHITE);
        rectProps.set(AnimationPropertiesKeys.DEPTH_PROPERTY, 2);
        this.headerRect = lang.newRect(new Offset(-5, -5, "header", AnimalScript.DIRECTION_NW), new Offset(5, 5, "header", AnimalScript.DIRECTION_SE), "headerRect", null, rectProps);
        // create empty start page
        this.lang.nextStep();
        // setup start page with description
        this.lang.newText(new Coordinates(10, 100), "", "description1", null, this.textProps);
        this.lang.newText(new Offset(0, 3, "description1", AnimalScript.DIRECTION_SW), "Die optische Triangulation ist eine geometrische Methode um Abstandsmessungen durchzuführen.", "description2", null, this.textProps);
        this.lang.newText(new Offset(0, 3, "description2", AnimalScript.DIRECTION_SW), "Dies geschieht durch die genaue Winkelmessung innerhalb von Dreiecken und die Berechnung erfolgt mittels trigonometrischer Funktionen.", "description3", null, this.textProps);
        this.lang.newText(new Offset(0, 3, "description3", AnimalScript.DIRECTION_SW), "Aus diesem Grund heißt das Verfahren auch Triangulation.", "description4", null, this.textProps);
        this.lang.newText(new Offset(0, 3, "description4", AnimalScript.DIRECTION_SW), "Allgemein kann gesagt werden, dass anhand von zwei bekannten Punkten der Abstand zu beliebigen Punkten im Raum bestimmt werden kann.", "description5", null, this.textProps);
        this.lang.newText(new Offset(0, 3, "description5", AnimalScript.DIRECTION_SW), "Bei dem hier umgesetzten Verfahren handelt es sich um ein aktives Verfahren, da eine Lichtquelle genutzt wird.", "description6", null, this.textProps);
        this.lang.newText(new Offset(0, 3, "description6", AnimalScript.DIRECTION_SW), "Das Verfahren kann auch erweitert werden. So kann zum Beispiel ein Muster projiziert", "description7", null, this.textProps);
        this.lang.newText(new Offset(0, 3, "description7", AnimalScript.DIRECTION_SW), "und damit die Entfernung von allen Punkten des Musters gleichzeitig gemessen werden.", "description8", null, this.textProps);
        this.lang.newText(new Offset(0, 3, "description8", AnimalScript.DIRECTION_SW), "Allerdings verwenden wir hier eine Methode mit einzelnen Laserstrahlen.", "description9", null, this.textProps);
        this.lang.nextStep("Einleitung"); // create a new step with label
        // hide every description and show title(-box) again
        this.lang.hideAllPrimitives();
        this.header.show();
        this.headerRect.show();
        // draw coordinate system
        PolylineProperties coordinateProperties = new PolylineProperties();
        coordinateProperties.set(AnimationPropertiesKeys.COLOR_PROPERTY, Color.BLACK);
        coordinateProperties.set(AnimationPropertiesKeys.FWARROW_PROPERTY, true);
        lang.newPolyline(new Coordinates[] { new Coordinates(5, OFFSET - MIRROR_LASER_OFFSET - LASER_HEIGHT - 15), new Coordinates(50, OFFSET - MIRROR_LASER_OFFSET - LASER_HEIGHT - 15) }, "x_axis", null, coordinateProperties);
        lang.newText(new Offset(5, 0, "x_axis", AnimalScript.DIRECTION_E), "X", "x_axis_text", null, textProps);
        lang.newPolyline(new Coordinates[] { new Coordinates(5, OFFSET - MIRROR_LASER_OFFSET - LASER_HEIGHT - 15), new Coordinates(5, OFFSET - MIRROR_LASER_OFFSET - LASER_HEIGHT + 50 - 15) }, "y_axis", null, coordinateProperties);
        lang.newText(new Offset(0, 5, "y_axis", AnimalScript.DIRECTION_S), "Y", "y_axis_text", null, textProps);

        // draw laser
        this.laserProps.set(AnimationPropertiesKeys.FILLED_PROPERTY, true);
        lang.newRect(new Coordinates((int)laserPos.getX() - this.LASER_WIDTH, (int) laserPos.getY() + this.OFFSET - this.LASER_HEIGHT - this.MIRROR_LASER_OFFSET), new Coordinates((int)laserPos.getX() + this.LASER_WIDTH, (int)laserPos.getY() + this.OFFSET - this.MIRROR_LASER_OFFSET), "laser", null, this.laserProps);
        // draw camera
        lang.newPolyline(new Coordinates[] {
                            new Coordinates((int)laserPos.getX(), (int)(laserPos.getY() + this.b - this.HOLESIZE + this.OFFSET)),
                            new Coordinates((int)laserPos.getX(), (int)(laserPos.getY() + this.b + this.OFFSET - this.OVERSIZE - this.pictureChipSize / 2)),
                            new Coordinates((int)(laserPos.getX() - this.pictureHoleDistance - this.OVERSIZE), (int)(laserPos.getY() + this.b + this.OFFSET - this.OVERSIZE - this.pictureChipSize / 2)),
                            new Coordinates((int)(laserPos.getX() - this.pictureHoleDistance - this.OVERSIZE), (int)(laserPos.getY() + this.b + this.OVERSIZE + this.OFFSET + this.pictureChipSize / 2)),
                            new Coordinates((int)laserPos.getX(), (int)(laserPos.getY() + this.b + this.OVERSIZE + this.OFFSET + this.pictureChipSize / 2)),
                            new Coordinates((int)laserPos.getX(), (int)(laserPos.getY() + this.b + this.HOLESIZE + this.OFFSET))}, "CameraHole", null, this.cameraProps);
        // draw camera chip
        PolylineProperties chipProps = new PolylineProperties();
        chipProps.set(AnimationPropertiesKeys.COLOR_PROPERTY, Color.LIGHT_GRAY);
        lang.newPolyline(new Coordinates[] {
                            new Coordinates((int)(laserPos.getX() - this.pictureHoleDistance), (int)(laserPos.getY() + this.b + this.OFFSET - this.pictureChipSize / 2)),
                            new Coordinates((int)(laserPos.getX() - this.pictureHoleDistance), (int)(laserPos.getY() + this.b + this.OFFSET + this.pictureChipSize / 2))
        }, "Chip", null, chipProps);
        // draw object
        lang.newCircle(new Coordinates((int)objPos.getX(), (int)objPos.getY() + this.OFFSET), (int)this.radius, "object", null, this.circleProps);

        // add source code
        this.sourceCode = lang.newSourceCode(new Coordinates(10, 70), "sourceCode", null, this.sourceCodeProps);
        this.sourceCode.addCodeLine("1. Richte Spiegel aus (addiere Spiegelwinkel-Schrittweite auf vorherigen Winkel)", null, 0, null); // 0
        this.sourceCode.addCodeLine("2. Schieße Laser, falls Objekt nicht getroffen springe zu 1.", null, 0, null); // 1
        this.sourceCode.addCodeLine("3. Schieße Laser, falls das Objekt getroffen wurde: ", null, 0, null); // 2
        this.sourceCode.addCodeLine("3* Bestimme Pixel, auf dem der Laser landet", null, 1, null); // 3
        this.sourceCode.addCodeLine("3* Bestimme durch Spiegelwinkel und Laser-Chip-Schnittpunkt den abgetasteten Punkt", null, 1, null); // 4
       
        // add chipsize text
        lang.newText(new Coordinates((int)(laserPos.getX() - this.pictureHoleDistance - this.OVERSIZE - 40), (int)(laserPos.getY() + this.b + this.OVERSIZE + this.OFFSET + this.pictureChipSize / 2 - 10)), (this.chipResolution - 1) + "p", "chipResolutionInfo", null, textProps);
        lang.newText(new Coordinates((int)(laserPos.getX() - this.pictureHoleDistance - this.OVERSIZE - 20), (int)(laserPos.getY() + this.b + this.OFFSET - this.OVERSIZE - this.pictureChipSize / 2)), "0p", "chipResolutionInfo1", null, textProps);

        lang.nextStep("Starte Laser");
        this.loop();
        lang.nextStep();

        // outro
        this.lang.hideAllPrimitives();
        this.header.show();
        this.headerRect.show();

        this.lang.newText(new Coordinates(10, 100), "Nachdem der Laser über alle Spiegeleinstellungen geschossen wurde ist nun der Algrithmus beendet.", "outro1", null, this.textProps);
        this.lang.newText(new Offset(0, 3, "outro1", AnimalScript.DIRECTION_SW), "Dadurch haben wir an einigen Stellen des Objekts die entsprechenden Tiefenwerte erhalten.", "outro2", null, this.textProps);
        this.lang.newText(new Offset(0, 3, "outro2", AnimalScript.DIRECTION_SW), "Je nach dem wie die Chipgröße, Auflösung und Basislänge gewählt wurden, ist der berechnete Wert ziemlich genau.", "outro3", null, this.textProps);
        this.lang.newText(new Offset(0, 3, "outro3", AnimalScript.DIRECTION_SW), "Allerdings kann es auch zu kleinen Abweichungen durch Rundungen oder durch die Abtastrate kommen.", "outro4", null, this.textProps);
        this.lang.newText(new Offset(0, 3, "outro4", AnimalScript.DIRECTION_SW), "In folgender Liste sind die Abweichungen von dem berechneten Wert zu den realen Werten aufgelistet. Die Koordinaten sind wie folg angegeben: (Tiefe, Höhe)", "outro5", null, this.textProps);
        
        MatrixProperties mp = new MatrixProperties();
        mp.set(AnimationPropertiesKeys.GRID_BORDER_COLOR_PROPERTY, Color.BLACK);
        mp.set(AnimationPropertiesKeys.COLOR_PROPERTY, Color.WHITE);
        mp.set(AnimationPropertiesKeys.FILLED_PROPERTY, false);
        mp.set(AnimationPropertiesKeys.GRID_ALIGN_PROPERTY, "center");
        mp.set(AnimationPropertiesKeys.GRID_STYLE_PROPERTY, "table");
        if(table.size() >= 2) {
            String[][] arTable = new String[table.size() / 2 + 1][3];
            arTable[0] = new String[] {"Realer Wert", "Berechneter Wert", "Delta"};
            int pos = 1;
            for(int i = 0; i < table.size(); i+=2) {
                arTable[pos] = new String[] {table.get(i).toString(), table.get(i+1).toString(), table.get(i).subtract(table.get(i+1)).toString()};
                pos++;
            }
            lang.newStringMatrix(new Offset(0, 20, "outro5", AnimalScript.DIRECTION_SW), arTable,"table", null, mp);
        } else {
            this.lang.newText(new Offset(0, 20, "outro5", AnimalScript.DIRECTION_SW), "Es konnten keine validen Tiefenwerte berechnet werden!", "outro6", null, this.textProps);
        }
        
        this.lang.nextStep("Zusammenfassung");

        this.lang.finalizeGeneration();
        return lang.toString();
    }

    /**
     * Calculates the depth of an object
     * @param pix the pixel of the laser position
     * @return depth of the object at the given laser position
     */
    public double triangulate(int pix) {
        // calculate beta
        double beta = Math.atan(this.getTanBeta(pix));

        // calculate alpha
        double alpha = 2*(90 - mirrorAngle + 45) - 90;

        // calculate depth
        double z = this.b / (Math.tan(degreeToRadians(alpha)) + Math.tan(beta));
        return z;
    }

    /**
     * Fires the laser towards the mirror and hopefully against the object
     * @param mirrorVector the vector of the mirror
     * @return returns the calculated pixel position of the laser on the camera chip (returns -1 when no collision with the object)
     */
    public int fireLaser(Vector mirrorVector) {
        int pixel = -1;
        double angle = mirrorVector.calculateAngle(Vector.NEGATIVE_Y_AXIS); // angle in radians
        Vector laserEndPoint = new Vector(this.laserPos.getX(), -1400).rotateAroundPoint(this.laserPos, -(2*angle + Math.PI));
        Vector[] collisionArray = new Vector(this.laserPos, laserEndPoint).checkCollisionWithCircle(this.laserPos, this.objPos, this.radius);
        Vector collision = collisionArray == null ? null : collisionArray[0];
        PointProperties pp = new PointProperties();
        pp.set(AnimationPropertiesKeys.COLOR_PROPERTY, Color.BLUE);
        pp.set(AnimationPropertiesKeys.DEPTH_PROPERTY, 0);

        Polyline laser1, laser2;
        Text hitText = null;
        laser2 = null;
        sourceCode.unhighlight(0);
        sourceCode.highlight(1);
        // if there is a collision with the object, create laser from collision point to chip
        if(collision != null) {
            // draw laser from mirror to circle
            laser1 = lang.newPolyline(new Coordinates[]{this.laserPos.offset(0,OFFSET).toCoordinates(), collision.offset(0, OFFSET).toCoordinates()}, "laser", null, laserLineProps);
            // check if the vision to the collision point is blocked
            Vector[] returnCollision = new Vector(laserPos.offset(0,this.b), collision).checkCollisionWithCircle(laserPos.offset(0, this.b), this.objPos, this.radius);
            // if the laser hits the backside of the object..
            if(collision.getX() > this.objPos.getX() || !returnCollision[0].equals(collision)) {
                // show that the vision of the camera is blocked,
                hitText = lang.newText(this.objPos.offset(this.radius + 5, OFFSET).toCoordinates(), "Der Laser trifft das Objekt, aber das Objekt verdeckt die Sicht der Kamera!", "chipResolutionInfo2", null, textProps);
            }
            // or else send the laser to the camera.
            else {
                Vector laser = this.laserPos.offset(0, this.b).subtract(collision);
                double beta = laser.calculateAngle(Vector.X_AXIS);
                double chipHit = Math.tan(beta) * this.pictureHoleDistance;

                // fixing the chipHit direction, by checking if the y coordinate signs are equal
                Vector incomingLaser = new Vector(collision.getX(), collision.getY() + OFFSET,(laserPos.getX()), (laserPos.getY() + OFFSET + b));
                if(incomingLaser.getY() < 0 && chipHit > 0 || incomingLaser.getY() > 0 && chipHit < 0) chipHit = -chipHit;
                    pixel = (int) (chipHit * this.chipResolution / this.pictureChipSize + (this.chipResolution - 1) / 2); // calculate the hit pixel
                if(pixel < chipResolution && pixel >= 0) {
                    // draw second laser line to chip and show which pixel was hit
                    hitText = lang.newText(new Coordinates((int)(laserPos.getX() - this.pictureHoleDistance - this.OVERSIZE - 40), (int)(laserPos.getY() + this.b + this.OVERSIZE + this.OFFSET + this.pictureChipSize / 2 + 10)), "Chip Hit: Pixel " + pixel, "chipResolutionInfo2", null, textProps);
                    laser2 = lang.newPolyline(new Coordinates[]{new Coordinates((int)collision.getX(), (int)collision.getY() + OFFSET), new Coordinates((int)(laserPos.getX() - this.pictureHoleDistance), (int)(laserPos.getY() + OFFSET + b + chipHit))}, "kameraLaser", null, laserLineProps);
                    table.add(collision);
                } else {
                    // draw second laser line to hole and explain that the chip was missed
                    hitText = lang.newText(new Coordinates((int)(laserPos.getX() - this.pictureHoleDistance - this.OVERSIZE - 40), (int)(laserPos.getY() + this.b + this.OVERSIZE + this.OFFSET + this.pictureChipSize / 2 + 10)), "Laser trifft nicht den Chip", "chipResolutionInfo2", null, textProps);
                    laser2 = lang.newPolyline(new Coordinates[]{new Coordinates((int)collision.getX(), (int)collision.getY() + OFFSET), new Coordinates((int)(laserPos.getX()), (int)(laserPos.getY() + OFFSET + b))}, "kameraLaser", null, laserLineProps);
                    pixel = -1;
                }

                sourceCode.unhighlight(1);
                sourceCode.highlight(2);
                sourceCode.highlight(3);
            }
        } else { // else send the laser into oblivion
            laser1 = lang.newPolyline(new Coordinates[]{this.laserPos.offset(0,OFFSET).toCoordinates(), laserEndPoint.offset(0,OFFSET).toCoordinates()}, "laser", null, laserLineProps);
        }
        lang.nextStep();
        lastLaser1 = laser1;
        this.staticLaser.changeColor(AnimalScript.COLORCHANGE_COLOR, Color.LIGHT_GRAY, null, null);
        if(laser2 != null) lastLaser2 = laser2;
        if(hitText != null) hitText.hide();
        return pixel;
    }

    /**
     * Animation loop
     */
    private void loop() {
        Vector mirrorPos1 = new Vector(this.laserPos.getX() - LASER_WIDTH, this.laserPos.getY()).rotateAroundPoint(this.laserPos, degreeToRadians(this.mirrorAngle-45));
        Vector mirrorPos2 = new Vector(this.laserPos.getX() + LASER_WIDTH, this.laserPos.getY()).rotateAroundPoint(this.laserPos, degreeToRadians(this.mirrorAngle-45));
        
        double radians = degreeToRadians(mirrorStepResolution);
        Polyline mirror;
        PolylineProperties plProps = new PolylineProperties(); // create mirror props
        plProps.set(AnimationPropertiesKeys.COLOR_PROPERTY, Color.GRAY);
        plProps.set(AnimationPropertiesKeys.DEPTH_PROPERTY, 2);
        // draw static laser
        staticLaser = lang.newPolyline(new Coordinates[] { laserPos.offset(0, OFFSET).toCoordinates(), laserPos.offset(0,  -MIRROR_LASER_OFFSET + OFFSET).toCoordinates()}, "laserMirror", null, laserLineProps);
        while(this.mirrorAngle - this.mirrorStepResolution >= 45) {  // shoot laser while it is in the appropriate area
            // draw mirror
            sourceCode.unhighlight(1);
            sourceCode.highlight(0);
            mirror = lang.newPolyline(new Coordinates[] {mirrorPos1.offset(0,OFFSET).toCoordinates(), mirrorPos2.offset(0,OFFSET).toCoordinates()}, "mirror" + mirrorAngle, null, plProps);
            lang.nextStep();
            int pix = fireLaser(mirrorPos1.subtract(this.laserPos));// calculate resulting pixel
            if(pix != -1){ // check if laser collided with object
                sourceCode.unhighlight(3);
                sourceCode.highlight(4);
                double z = triangulate(pix); // calculate the depth of the object
                double x =  this.b + this.laserPos.getY() - z * this.getTanBeta(pix);
                table.add(new Vector(z + this.laserPos.getX(), x));
                // draw angles
                int gammaValue = (int) Math.abs(90 - mirrorAngle + 45);
                int alphaValue = (int) Math.abs((2*(90 - mirrorAngle + 45) - 90));
                int betaValue = (int) radiansToDegrees(Math.atan(getTanBeta(pix)));

                // gamma
                arcProps.set(AnimationPropertiesKeys.STARTANGLE_PROPERTY, (360-gammaValue) % 360);
                arcProps.set(AnimationPropertiesKeys.ANGLE_PROPERTY, gammaValue);
                Arc angleGamma = lang.newArc(this.laserPos.offset(0, OFFSET).toCoordinates(), new Coordinates((int) (new Vector(laserPos, objPos).magnitude() / 4),(int) (new Vector(laserPos, objPos).magnitude()/4)), "gamma", null, arcProps); // mirrorangle
                // alpha
                arcProps.set(AnimationPropertiesKeys.STARTANGLE_PROPERTY, (360-alphaValue) % 360);
                arcProps.set(AnimationPropertiesKeys.ANGLE_PROPERTY, alphaValue);
                Arc angleAlpha = lang.newArc(this.laserPos.offset(0, OFFSET).toCoordinates(), new Coordinates((int)(new Vector(laserPos, objPos).magnitude() / 2),(int)(new Vector(laserPos, objPos).magnitude() / 2)), "alpha", null, arcProps);
                // beta
                arcProps.set(AnimationPropertiesKeys.STARTANGLE_PROPERTY, betaValue >= 0 ? 0 : (360 + betaValue) % 360);
                arcProps.set(AnimationPropertiesKeys.ANGLE_PROPERTY, Math.abs(betaValue));
                Arc angleBeta = lang.newArc(this.laserPos.offset(0, OFFSET + this.b).toCoordinates(), new Coordinates((int)(new Vector(laserPos.offset(0, this.b), objPos).magnitude() / 2), (int)(new Vector(laserPos.offset(0, this.b), objPos).magnitude() / 2)), "beta", null, arcProps);// angle of laser going towards the camera

                // draw extensions for angle visualization 
                Coordinates cord1 = new Vector(0, laserPos.getY()).rotateAroundPoint(laserPos, degreeToRadians(90-this.mirrorAngle+45)).offset(0,OFFSET).toCoordinates();
                Coordinates cord2 = new Vector(1500, laserPos.getY()).rotateAroundPoint(laserPos, degreeToRadians((90-this.mirrorAngle+45))).offset(0,OFFSET).toCoordinates();
                Polyline extendedMirror = lang.newPolyline(new Coordinates[]{cord1, cord2}, "extendedMirror", null, this.angleLineProps);
                Polyline cameraExtension = lang.newPolyline(new Coordinates[] {new Vector(0, laserPos.getY()).offset(0, OFFSET + this.b).toCoordinates(), new Vector(1500, laserPos.getY()).offset(0, OFFSET + this.b).toCoordinates()}, "cameraExtension", null, this.angleLineProps);
                Polyline laserExtension = lang.newPolyline(new Coordinates[] {new Vector(0, laserPos.getY()).offset(0, OFFSET).toCoordinates(), new Vector(1500, laserPos.getY()).offset(0, OFFSET).toCoordinates()}, "laserExtension", null, this.angleLineProps);

                Text gamma = lang.newText(new Offset(-20, 0, angleGamma, AnimalScript.DIRECTION_E), "ɣ", "gammaText", null, this.textProps);
                Text alpha = lang.newText(new Offset(-20, 0, angleAlpha, AnimalScript.DIRECTION_E), "α", "alphaText", null, this.textProps);
                Text beta = lang.newText(new Offset(-20, 0, angleBeta, AnimalScript.DIRECTION_E), "β", "betaText", null, this.textProps);

                // show result with the calculation
                RectProperties bgProps = new RectProperties();
                bgProps.set(AnimationPropertiesKeys.FILL_PROPERTY, Color.WHITE);
                bgProps.set(AnimationPropertiesKeys.FILLED_PROPERTY, true);
                bgProps.set(AnimationPropertiesKeys.COLOR_PROPERTY, Color.BLACK);
                Rect background = lang.newRect(new Coordinates((int) (this.objPos.getX() + this.radius + 20), (int) this.objPos.getY() + this.OFFSET), new Coordinates((int) (this.objPos.getX() + this.radius + 600), (int) this.objPos.getY() + 170 + this.OFFSET), "background", null, bgProps);
                Text given1 = lang.newText(new Offset(10, 10, background, AnimalScript.DIRECTION_NW), "Spiegelwinkel: " + this.mirrorAngle + "; Pixel: " + pix, "given1", null, this.textProps);
                Text given2 = lang.newText(new Offset(0, 3, given1, AnimalScript.DIRECTION_SW), "Kameraverschiebung: " + "(" + laserPos.getX() + "," + (laserPos.getY() + b) + ")", "given2", null, this.textProps);
                Text calculation = lang.newText(new Offset(0, 3, given2, AnimalScript.DIRECTION_SW), "Bestimme Tiefenwert: z = basislänge / (tan(alpha) + tan(beta))", "calculation1" , null, this.textProps);
                Text calculation2 = lang.newText(new Offset(0, 3, calculation, AnimalScript.DIRECTION_SW), "alpha = 2 * spiegelwinkel - 90 = " + alphaValue, "calculation2" , null, this.textProps);
                Text calculation3 = lang.newText(new Offset(0, 3, calculation2, AnimalScript.DIRECTION_SW), "beta = atan((pixel - (chipAuflösung - 1)/2) / chipAuflösung/2)) = " + betaValue, "calculation3" , null, this.textProps);
                Text resultZ = lang.newText(new Offset(0, 3, calculation3, AnimalScript.DIRECTION_SW), "Berechnete Tiefe z (Aus Sicht der Kamera) = " + z, "result", null, this.textProps);
                Text objectZ = lang.newText(new Offset(0, 3, resultZ, AnimalScript.DIRECTION_SW), "x-Koordinate des Schnittpunktes = z + KameraverschiebungX = " + (z + this.laserPos.getX()), "x-coordinate", null, this.textProps);
                Text calculation4 = lang.newText(new Offset(0, 3, objectZ, AnimalScript.DIRECTION_SW), "y-Koordinate des Schnittpunktes = KameraverschiebungY - z * tan(beta) = " + x, "calculation4" , null, this.textProps);
                
                // hide the result and descriptions
                lang.nextStep();
                lastLaser2.changeColor(AnimalScript.COLORCHANGE_COLOR, Color.LIGHT_GRAY, null, null);
                background.hide();
                given1.hide();
                given2.hide();
                calculation.hide();
                calculation2.hide();
                calculation3.hide();
                resultZ.hide();
                objectZ.hide();
                calculation4.hide();
                angleBeta.hide();
                angleGamma.hide();
                angleAlpha.hide();
                gamma.hide();
                beta.hide();
                alpha.hide();
                extendedMirror.hide();
                cameraExtension.hide();
                laserExtension.hide();
                sourceCode.unhighlight(2);
                sourceCode.unhighlight(4);
            }
            lastLaser1.changeColor(AnimalScript.COLORCHANGE_COLOR, Color.LIGHT_GRAY, null, null);
            Color col = (Color) laserLineProps.get(AnimationPropertiesKeys.COLOR_PROPERTY);
            this.staticLaser.changeColor(AnimalScript.COLORCHANGE_COLOR, col, null, null);
            this.mirrorAngle -= this.mirrorStepResolution; // calculate next angle
            mirror.hide(); // hide old mirror
            // calculate new rotation of the mirror
            mirrorPos1 =  mirrorPos1.rotateAroundPoint(this.laserPos, radians);
            mirrorPos2 =  mirrorPos2.rotateAroundPoint(this.laserPos, radians);
        }
    }

    /**
     * calculates the angle of the incoming laser
     * @param pix the pix which is hit in the camera
     * @return returns the angle
     */
    private double getTanBeta(int pix) {
        double opposite = (pix - (chipResolution-1)/2) * (pictureChipSize/chipResolution);
        double adjacent = pictureHoleDistance;
        return opposite/adjacent;
    }

    /**
    * convertes radians to degrees
    * @param theta the angle to convert
    * @return returns the angle in degrees
    */
    private double radiansToDegrees(double theta) {
        return theta * 180 / Math.PI;
    }

    /**
     * convertes degrees to radians
     * @param theta the angle to convert
     * @return returns the angle in radians
     */
    private double degreeToRadians(double theta) {
        return theta * Math.PI / 180;
    }

    /**
     * returns the name of the generator
     */
    @Override
    public String getName() {
        return "Optische Triangulation [DE]";
    }

    /**
     * returns the name of the algorithm
     */
    @Override
    public String getAlgorithmName() {
        return "Optische Triangulation [DE]";
    }

    /**
     * returns the name of the authors
     */
    @Override
    public String getAnimationAuthor() {
        return "Pascal Schikora und Egemen Ulutürk";
    }

    /**
     * returns the description of the algorithm
     */
    @Override
    public String getDescription(){
        return DESCRIPTION;
    }

    /**
     * returns example code
     */
    @Override
    public String getCodeExample(){
        return PSEUDO_CODE;
    }

    /**
     * returns the file extension of the generated file
     */
    @Override
    public String getFileExtension(){
        return "asu";
    }

    /**
     * returns the language, which was used to explain the algorithm
     */
    @Override
    public Locale getContentLocale() {
        return Locale.GERMAN;
    }

    /**
     * returns the type of the generator
     */
    @Override
    public GeneratorType getGeneratorType() {
        return new GeneratorType(GeneratorType.GENERATOR_TYPE_GRAPHICS);
    }
    /**
     * returns the language type of the generator
     */
    @Override
    public String getOutputLanguage() {
        return Generator.PSEUDO_CODE_OUTPUT;
    }
}
