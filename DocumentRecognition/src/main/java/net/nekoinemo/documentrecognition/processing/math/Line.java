package net.nekoinemo.documentrecognition.processing.math;

import org.opencv.core.*;

public class Line {

	private static double NEAR_DISTANCE_NORMAL = 0.1d;
	private static double DEFAULT_NEAR_DISTANCE = 30;
	private static Scalar DEFAULT_COLOUR = new Scalar(255, 255, 255);
	private static int DEFAULT_THICKNESS = 1;
	private static Point HORIZONTAL = new Point(1, 0);
	private static Point VERTICAL = new Point(0, 1);

	Point point1;
	Point point2;

	public Line(double x1, double y1, double x2, double y2) {

		if (x1 < x2) {
			point1 = new Point(x1, y1);
			point2 = new Point(x2, y2);
		} else {
			point1 = new Point(x2, y2);
			point2 = new Point(x1, y1);
		}
	}

	public Point getPoint2() {

		return point2;
	}
	public Point getPoint1() {

		return point1;
	}
	public double length(){

		return MathHelper.distance(point1, point2);
	}

	public boolean isNear(Line line) {

		return isNear(line, DEFAULT_NEAR_DISTANCE);
	}
	public boolean isNear(Line line, double nearDistance) {

		// This line ends & other line
		double a1 = MathHelper.distance(line, this.point1);
		double a2 = MathHelper.distance(line, this.point2);
		double al = MathHelper.distance(line.point1, line.point2);
		// Other line ends & this line
		double b1 = MathHelper.distance(this, line.point1);
		double b2 = MathHelper.distance(this, line.point2);
		double bl = MathHelper.distance(this.point1, this.point2);

		// Both ends are close to other line
		if (a1 < nearDistance && a2 < nearDistance) return true;
		if (b1 < nearDistance && b2 < nearDistance) return true;

		// One end is close and distance from other is not greater then line length
		if (a1 < nearDistance && a2 < al) return true;
		if (a2 < nearDistance && a1 < al) return true;
		if (b1 < nearDistance && b2 < bl) return true;
		if (b2 < nearDistance && b1 < bl) return true;
		return false;
	}
	public boolean intersects(Line line) {

		return intersects(line, null);
	}
	public boolean intersects(Line line, Point intersection) {

		double x1d = point2.x - point1.x;
		double y1d = point2.y - point1.y;
		double x2d = line.point2.x - line.point1.x;
		double y2d = line.point2.y - line.point1.y;

		double s = (-y1d * (point1.x - line.point1.x) + x1d * (point1.y - line.point1.y)) / (-x2d * y1d + x1d * y2d);
		double t = (x2d * (point1.y - line.point1.y) - y2d * (point1.x - line.point1.x)) / (-x2d * y1d + x1d * y2d);

		if (s >= 0 && s <= 1 && t >= 0 && t <= 1) {
			if (intersection != null) {
				intersection.x = point1.x + (t * x1d);
				intersection.y = point1.y + (t * y1d);
			}
			return true; // Collision detected
		}

		return false; // No collision
	}
	/**
	 * Checks if passed line and this line are both horizontally oriented and their ends' X are the same (within certain margin)
	 *
	 * @param line
	 * @param margin
	 *
	 * @return
	 */
	public boolean isHorizontalPair(Line line, double margin) {

		if (!this.normalAbs().equals(line.normalAbs())) return false;

		return (Math.abs(this.getPoint1().x - line.getPoint1().x) < margin && Math.abs(this.getPoint2().x - line.getPoint2().x) < margin) || (Math.abs(this.getPoint1().x - line.getPoint2().x) < margin && Math.abs(this.getPoint2().x - line.getPoint1().x) < margin);
	}

	public Point normal() {

		Point delta = new Point(point2.x - point1.x, point2.y - point1.y);
		double length = MathHelper.distance(delta, new Point(0, 0));

		return new Point(delta.x / length, delta.y / length);
	}
	public Point normalAbs() {

		Point normal = normal();
		return new Point(Math.abs(normal.x), Math.abs(normal.y));
	}
	public boolean sameOrientation(Point normalAbs) {

		return MathHelper.distance(normalAbs(), normalAbs) < NEAR_DISTANCE_NORMAL;
	}
	public boolean isHorizontal() {

		return sameOrientation(HORIZONTAL);
	}
	public boolean isVertical() {

		return sameOrientation(VERTICAL);
	}

	public void draw(Mat image) {

		draw(image, DEFAULT_COLOUR, DEFAULT_THICKNESS);
	}
	public void draw(Mat image, Scalar colour, int thickness) {

		Core.line(image, point1, point2, colour, thickness);
	}

	@Override
	public String toString() {

		return "Line{" +
				"point1=" + point1 +
				", point2=" + point2 +
				'}';
	}
}
