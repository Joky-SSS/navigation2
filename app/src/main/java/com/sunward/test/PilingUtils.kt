package com.sunward.test

import kotlin.math.*

object PilingUtils {
    object WGS84 {
        const val a = 6378137.0
        const val b = 6356752.3142451
        const val f = 1 / 298.257
        val e = sqrt(a.pow(2) - b.pow(2)) / a
        val e1 = sqrt(a.pow(2) - b.pow(2)) / b

        //Method 1
        val scale = 1
        val falseNorthing = 0
        val falseEasting = 0
        val e2 = (a.pow(2) - WGS84.b.pow(2)) / a.pow(2)
        val n = (a - WGS84.b) / (a + WGS84.b).roundFraction(16)
        val aRoof = a / (1.0 + n) * (1.0 + n.pow(2) / 4.0 + n.pow(4) / 64.0)
        val A = e2
        val B = (5.0 * e2.pow(2) - e2.pow(3)) / 6.0
        val C = (104.0 * e2.pow(3) - 45.0 * e2.pow(4)) / 120.0
        val D = (1237.0 * e2.pow(4)) / 1260.0
        val beta1 = n / 2.0 - 2.0 * n.pow(2) / 3.0 + 5.0 * n.pow(3) / 16.0 + 41.0 * n.pow(4) / 180.0
        val beta2 = 13.0 * n.pow(2) / 48.0 - 3.0 * n.pow(3) / 5.0 + 557.0 * n.pow(4) / 1440.0
        val beta3 = 61.0 * n.pow(3) / 240.0 - 103.0 * n.pow(4) / 140.0
        val beta4 = 49561.0 * n.pow(4) / 161280.0

        //Method 2
        val m0 = a * (1 - e.pow(2));
        val m2 = 3 * e.pow(2) * m0 / 2;
        val m4 = 5 * e.pow(2) * m2 / 4;
        val m6 = 7 * e.pow(2) * m4 / 6;
        val m8 = 9 * e.pow(2) * m6 / 8;

        val a0 = m0 + m2 / 2 + 3 * m4 / 8 + 5 * m6 / 16 + 35 * m8 / 128;
        val a2 = m2 / 2 + m4 / 2 + 15 * m6 / 32 + 7 * m8 / 16;
        val a4 = m4 / 8 + 3 * m6 / 16 + 7 * m8 / 32;
        val a6 = m6 / 32 + m8 / 16;
        val a8 = m8 / 128;
    }

    fun distance(from: Point, to: Point): Double {
        return distance(from.x, from.y, to.x, to.y)
    }

    fun distance(
        x1: Double,
        y1: Double,
        x2: Double,
        y2: Double
    ): Double {
        val distanceX = x2 - x1
        val distanceY = y2 - y1
        return sqrt(
            abs(distanceX * distanceX) + abs(distanceY * distanceY)
        )
    }

    val lambdaZeroArc = 114.deg2arc()
    fun bl2gsXY(B: Double, L: Double): Point {
        val phi = B.deg2arc()
        val lambda = L.deg2arc()

        val deltaLambda = lambda - lambdaZeroArc

        //Method 1
        /*
        val phiStar =
            phi - sin(phi) * cos(phi) * (WGS84.A + WGS84.B * sin(phi).pow(2) + WGS84.C * sin(phi).pow(
                4
            ) + WGS84.D * sin(
                phi
            ).pow(6))
        val xiPrim = atan(tan(phiStar) / cos(deltaLambda))
        val etaPrim = atanh(cos(phiStar) * sin(deltaLambda))
        val x = WGS84.scale * WGS84.aRoof * (xiPrim
                + WGS84.beta1 * sin(2.0 * xiPrim) * cosh(2.0 * etaPrim)
                + WGS84.beta2 * sin(4.0 * xiPrim) * cosh(4.0 * etaPrim)
                + WGS84.beta3 * sin(6.0 * xiPrim) * cosh(6.0 * etaPrim)
                + WGS84.beta4 * sin(8.0 * xiPrim) * cosh(8.0 * etaPrim)) + WGS84.falseNorthing
        val y = WGS84.scale * WGS84.aRoof * (etaPrim
                + WGS84.beta1 * cos(2.0 * xiPrim) * sinh(2.0 * etaPrim)
                + WGS84.beta2 * cos(4.0 * xiPrim) * sinh(4.0 * etaPrim)
                + WGS84.beta3 * cos(6.0 * xiPrim) * sinh(6.0 * etaPrim)
                + WGS84.beta4 * cos(8.0 * xiPrim) * sinh(8.0 * etaPrim)) + WGS84.falseEasting
        */


        //Method 2
        val m = cos(phi) * deltaLambda;
        val v = WGS84.e1 * cos(phi);
        val t = tan(phi);
        val N = WGS84.a / sqrt(1 - WGS84.e.pow(2) * (sin(phi)).pow(2));

        val X =
            WGS84.a0 * phi - WGS84.a2 * sin(2 * phi) / 2 + WGS84.a4 * sin(4 * phi) / 4 - WGS84.a6 * sin(
                6 * phi
            ) / 6 + WGS84.a8 * sin(8 * phi) / 8;

        val x2 =
            X + N * t * (m.pow(2) / 2 + (5 - t.pow(2) + 9 * v.pow(2) + 4 * v.pow(4)) * m.pow(4) / 24 + (61 - 58 * t.pow(
                2
            ) + t.pow(4)) * m.pow(6) / 720)
        val y2 =
            N * (m + (1 - t.pow(2) + v.pow(2)) * m.pow(3) / 6 + (5 - 18 * t.pow(2) + t.pow(4) + 14 * v.pow(
                2
            ) - 58 * v.pow(2) * t.pow(2)) * m.pow(5) / 120)

        return Point(x2, y2)
    }


}
