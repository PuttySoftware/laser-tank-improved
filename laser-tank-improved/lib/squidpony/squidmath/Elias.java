package squidpony.squidmath;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import squidpony.annotation.Beta;

/**
 * Contains methods to draw antialiased lines based on floating point
 * coordinates.
 *
 * Because of the way this line is calculated, endpoints may be swapped and
 * therefore the list may not be in start-to-end order.
 *
 * Based on work by Hugo Elias at
 * http://freespace.virgin.net/hugo.elias/graphics/x_wuline.htm which is in turn
 * base on work by Wu.
 *
 * @author Eben Howard - http://squidpony.com - howard@squidpony.com
 */
@Beta
public class Elias implements Serializable {
    private static final long serialVersionUID = 5290834334572814012L;
    private List<Coord> path;
    private float[][] lightMap;
    private int width, height;
    private double threshold = 0.0;

    public Elias() {
    }

    public synchronized float[][] lightMap(final double startx, final double starty, final double endx,
	    final double endy) {
	this.line(startx, starty, endx, endy);
	return this.lightMap;
    }

    /**
     * Gets the line between the two points.
     *
     * @param startx
     * @param starty
     * @param endx
     * @param endy
     * @return
     */
    public synchronized List<Coord> line(final double startx, final double starty, final double endx,
	    final double endy) {
	this.path = new LinkedList<>();
	this.width = (int) (Math.max(startx, endx) + 1);
	this.height = (int) (Math.max(starty, endy) + 1);
	this.lightMap = new float[this.width][this.height];
	this.runLine(startx, starty, endx, endy);
	return this.path;
    }

    /**
     * Gets the line between the two points.
     *
     * @param startx
     * @param starty
     * @param endx
     * @param endy
     * @param brightnessThreshold between 0.0 (default) and 1.0; only Points with
     *                            higher brightness will be included
     * @return
     */
    public synchronized List<Coord> line(final double startx, final double starty, final double endx, final double endy,
	    final double brightnessThreshold) {
	this.threshold = brightnessThreshold;
	this.path = new LinkedList<>();
	this.width = (int) (Math.max(startx, endx) + 1);
	this.height = (int) (Math.max(starty, endy) + 1);
	this.lightMap = new float[this.width][this.height];
	this.runLine(startx, starty, endx, endy);
	return this.path;
    }

    public synchronized List<Coord> line(final Coord start, final Coord end) {
	return this.line(start.x, start.y, end.x, end.y);
    }

    public synchronized List<Coord> line(final Coord start, final Coord end, final double brightnessThreshold) {
	return this.line(start.x, start.y, end.x, end.y, brightnessThreshold);
    }

    public synchronized List<Coord> getLastPath() {
	return this.path;
    }

    /**
     * Marks the location as having the visibility given.
     *
     * @param x
     * @param y
     * @param c
     */
    private void mark(final double x, final double y, final double c) {
	// check bounds overflow from antialiasing
	if (x >= 0 && x < this.width && y >= 0 && y < this.height && c > this.threshold) {
	    this.path.add(Coord.get((int) x, (int) y));
	    this.lightMap[(int) x][(int) y] = (float) c;
	}
    }

    private double trunc(final double x) {
	if (x < 0) {
	    return Math.ceil(x);
	} else {
	    return Math.floor(x);
	}
    }

    private double frac(final double x) {
	return x - this.trunc(x);
    }

    private double invfrac(final double x) {
	return 1 - this.frac(x);
    }

    private void runLine(final double startx, final double starty, final double endx, final double endy) {
	double x1 = startx, y1 = starty, x2 = endx, y2 = endy;
	double grad, xd, yd, xgap, xend, yend, yf, brightness1, brightness2;
	int x, ix1, ix2, iy1, iy2;
	boolean shallow = false;
	xd = x2 - x1;
	yd = y2 - y1;
	if (Math.abs(xd) > Math.abs(yd)) {
	    shallow = true;
	}
	if (!shallow) {
	    double temp = x1;
	    x1 = y1;
	    y1 = temp;
	    temp = x2;
	    x2 = y2;
	    y2 = temp;
	    xd = x2 - x1;
	    yd = y2 - y1;
	}
	if (x1 > x2) {
	    double temp = x1;
	    x1 = x2;
	    x2 = temp;
	    temp = y1;
	    y1 = y2;
	    y2 = temp;
	    xd = x2 - x1;
	    yd = y2 - y1;
	}
	grad = yd / xd;
	// add the first end point
	xend = this.trunc(x1 + .5);
	yend = y1 + grad * (xend - x1);
	xgap = this.invfrac(x1 + .5);
	ix1 = (int) xend;
	iy1 = (int) yend;
	brightness1 = this.invfrac(yend) * xgap;
	brightness2 = this.frac(yend) * xgap;
	if (shallow) {
	    this.mark(ix1, iy1, brightness1);
	    this.mark(ix1, iy1 + 1, brightness2);
	} else {
	    this.mark(iy1, ix1, brightness1);
	    this.mark(iy1 + 1, ix1, brightness2);
	}
	yf = yend + grad;
	// add the second end point
	xend = this.trunc(x2 + .5);
	yend = y2 + grad * (xend - x2);
	xgap = this.invfrac(x2 - .5);
	ix2 = (int) xend;
	iy2 = (int) yend;
	brightness1 = this.invfrac(yend) * xgap;
	brightness2 = this.frac(yend) * xgap;
	if (shallow) {
	    this.mark(ix2, iy2, brightness1);
	    this.mark(ix2, iy2 + 1, brightness2);
	} else {
	    this.mark(iy2, ix2, brightness1);
	    this.mark(iy2 + 1, ix2, brightness2);
	}
	// add the in-between points
	for (x = ix1 + 1; x < ix2; x++) {
	    brightness1 = this.invfrac(yf);
	    brightness2 = this.frac(yf);
	    if (shallow) {
		this.mark(x, (int) yf, brightness1);
		this.mark(x, (int) yf + 1, brightness2);
	    } else {
		this.mark((int) yf, x, brightness1);
		this.mark((int) yf + 1, x, brightness2);
	    }
	    yf += grad;
	}
    }
}
