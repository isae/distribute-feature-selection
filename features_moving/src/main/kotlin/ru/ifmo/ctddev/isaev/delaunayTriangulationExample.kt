package ru.ifmo.ctddev.isaev

import com.jogamp.opengl.*
import com.jogamp.opengl.awt.GLCanvas
import com.jogamp.opengl.util.FPSAnimator
import org.locationtech.jts.geom.*
import org.locationtech.jts.triangulate.IncrementalDelaunayTriangulator
import org.locationtech.jts.triangulate.quadedge.QuadEdgeSubdivision
import org.locationtech.jts.triangulate.quadedge.Vertex
import java.awt.Color
import java.awt.Dimension
import java.awt.Frame
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

const val WIDTH = 1024
const val HEIGHT = 768
/**
 * @author iisaev
 */
private val DIMENSION = Dimension(WIDTH, HEIGHT)

private val COLOR_TRIANGLE_FILL = Color(26, 121, 121)

private val COLOR_TRIANGLE_EDGES = Color(5, 234, 234)

private val COLOR_TRIANGLE_BORDER = Color(241, 241, 121)

private val COLOR_BACKGROUND = Color(47, 47, 47)

private val envelope = Envelope(Coordinate(-0.0, -0.0), Coordinate(WIDTH.toDouble(), HEIGHT.toDouble()))

private val subDiv = QuadEdgeSubdivision(envelope, 1E-6)

private val geometryFactory = GeometryFactory()

private val delaunayTriangulator = IncrementalDelaunayTriangulator(subDiv)

class DelaunayTriangulationExample : GLEventListener, MouseAdapter() {


    override fun init(drawable: GLAutoDrawable) {
        val gl = drawable.gl.gL2

        gl.glDisable(GL.GL_CULL_FACE)
        gl.glShadeModel(GL2.GL_SMOOTH)
        gl.glClearColor(COLOR_BACKGROUND.red / 255.0f, COLOR_BACKGROUND.green / 255.0f,
                COLOR_BACKGROUND.blue / 255.0f, 1f)
        gl.glClearDepth(1.0)
        gl.glEnable(GL.GL_DEPTH_TEST)
        gl.glDepthFunc(GL.GL_LEQUAL)
        gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_NICEST)

        gl.swapInterval = 1
        gl.glDisable(GL2.GL_CULL_FACE)
    }

    override fun reshape(drawable: GLAutoDrawable, x: Int, y: Int, width: Int, height: Int) {
        val gl = drawable.gl.gL2

        gl.glMatrixMode(GL2.GL_PROJECTION)
        gl.glLoadIdentity()
        gl.glOrtho(0.0, DIMENSION.getWidth(), DIMENSION.getHeight(), 0.0, 1.0, -1.0)
        gl.glMatrixMode(GL2.GL_MODELVIEW)
        gl.glLoadIdentity()
    }

    override fun display(drawable: GLAutoDrawable) {
        val gl = drawable.gl.gL2

        gl.glClear(GL.GL_COLOR_BUFFER_BIT or GL.GL_DEPTH_BUFFER_BIT)
        gl.glLoadIdentity()
        gl.glTranslatef(0.0f, 0.0f, 0.0f)

        gl.glLineWidth(1.0f)
        gl.glColor3ub(COLOR_TRIANGLE_FILL.red.toByte(), COLOR_TRIANGLE_FILL.green.toByte(),
                COLOR_TRIANGLE_FILL.blue.toByte())
        gl.glBegin(GL.GL_TRIANGLES)


        val trianglesGeom = subDiv.getTriangles(geometryFactory) as GeometryCollection
        val triangles = 0.until(trianglesGeom.numGeometries)
                .map { trianglesGeom.getGeometryN(it) }
                .map { Triangle(it.coordinates[0], it.coordinates[1], it.coordinates[2]) }
        triangles.forEach { triangle ->
            val a = triangle.p0
            val b = triangle.p1
            val c = triangle.p2

            gl.glVertex2d(a.x, a.y)

            gl.glVertex2d(b.x, b.y)
            gl.glVertex2d(c.x, c.y)
        }

        gl.glEnd()
        gl.glColor3ub(COLOR_TRIANGLE_EDGES.red.toByte(), COLOR_TRIANGLE_EDGES.green.toByte(),
                COLOR_TRIANGLE_EDGES.blue.toByte())
        gl.glBegin(GL.GL_LINES)

        triangles.forEach {
            gl.glVertex2d(it.p0.x, it.p0.y)
            gl.glVertex2d(it.p1.x, it.p1.y)
            gl.glVertex2d(it.p1.x, it.p1.y)
            gl.glVertex2d(it.p2.x, it.p2.y)
            gl.glVertex2d(it.p2.x, it.p2.y)
            gl.glVertex2d(it.p0.x, it.p0.y)
        }

        gl.glEnd()

        // draw all points
        gl.glPointSize(5.5f)
        gl.glColor3f(0.2f, 1.2f, 0.25f)

        gl.glColor3ub(COLOR_TRIANGLE_BORDER.red.toByte(), COLOR_TRIANGLE_BORDER.green.toByte(),
                COLOR_TRIANGLE_BORDER.blue.toByte())
        gl.glBegin(GL.GL_POINTS)


        triangles.forEach {
            gl.glVertex2d(it.p0.x, it.p0.y)
            gl.glVertex2d(it.p1.x, it.p1.y)
            gl.glVertex2d(it.p2.x, it.p2.y)
        }

        gl.glEnd()
    }

    override fun dispose(drawable: GLAutoDrawable) {}

    override fun mousePressed(e: MouseEvent) {
        val p = e.point
        delaunayTriangulator!!.insertSite(Vertex(p.x.toDouble(), p.y.toDouble()))
    }
}

fun main(args: Array<String>) {
    val frame = Frame("Delaunay Triangulation Example")
    frame.isResizable = false

    val caps = GLCapabilities(GLProfile.get("GL2"))
    caps.sampleBuffers = true
    caps.numSamples = 8

    val canvas = GLCanvas(caps)

    val ex = DelaunayTriangulationExample()
    canvas.addGLEventListener(ex)
    canvas.preferredSize = DIMENSION
    canvas.addMouseListener(ex)

    frame.add(canvas)

    val animator = FPSAnimator(canvas, 25)

    frame.addWindowListener(object : WindowAdapter() {
        override fun windowClosing(e: WindowEvent?) {
            Thread {
                animator.stop()
                System.exit(0)
            }.start()
        }
    })

    frame.isVisible = true
    frame.pack()
    animator.start()
}