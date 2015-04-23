package net.nekoinemo.documentrecognition.processing.math;

import org.opencv.core.Point;

public class MathHelper {

	public static double distance(Point point1, Point point2) {

		return Math.sqrt(distancePow2(point1, point2));
	}
	public static double distancePow2(Point point1, Point point2) {

		double x1d = point2.x - point1.x;
		double y1d = point2.y - point1.y;
		return x1d * x1d + y1d * y1d;
	}
	public static double distance(Line line, Point point) {

		return Math.sqrt(distancePow2(line, point));
	}
	public static double distancePow2(Line line, Point point) {

		double lengthPow2 = distancePow2(line.point2, line.point1);
		if (lengthPow2 == 0) return distancePow2(line.point1, point);

		double t = ((point.x - line.point1.x) * (line.point2.x - line.point1.x) + (point.y - line.point1.y) * (line.point2.y - line.point1.y)) / lengthPow2;

		if (t < 0) return distancePow2(point, line.point1);
		if (t > 1) return distancePow2(point, line.point2);
		return distancePow2(point, new Point(line.point1.x + t * (line.point2.x - line.point1.x), line.point1.y + t * (line.point2.y - line.point1.y)));
	}


}
