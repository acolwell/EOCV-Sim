package io.github.deltacv.easyvision.node.vision.overlay

import io.github.deltacv.easyvision.attribute.Attribute
import io.github.deltacv.easyvision.attribute.math.IntAttribute
import io.github.deltacv.easyvision.attribute.misc.ListAttribute
import io.github.deltacv.easyvision.attribute.vision.MatAttribute
import io.github.deltacv.easyvision.attribute.vision.PointsAttribute
import io.github.deltacv.easyvision.attribute.vision.ScalarAttribute
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.CodeGenSession
import io.github.deltacv.easyvision.codegen.GenValue
import io.github.deltacv.easyvision.codegen.parse.new
import io.github.deltacv.easyvision.codegen.parse.v
import io.github.deltacv.easyvision.node.Category
import io.github.deltacv.easyvision.node.DrawNode
import io.github.deltacv.easyvision.node.RegisterNode
import io.github.deltacv.easyvision.node.vision.Colors

@RegisterNode(
    name = "Draw Contours",
    category = Category.OVERLAY,
    description = "Draws the contours on a copy of the given image and outputs the result."
)
open class DrawContoursNode
@JvmOverloads constructor(val name: String = "Draw Contours", val isDrawOnInput: Boolean = false)
    : DrawNode<DrawContoursNode.Session>(name)  {

    val inputMat = MatAttribute(INPUT, "Input")
    val contours = ListAttribute(INPUT, PointsAttribute, "Contours")

    val lineColor = ScalarAttribute(INPUT, Colors.RGB, "Line Color")
    val lineThickness = IntAttribute(INPUT, "Line Thickness")

    val outputMat = MatAttribute(OUTPUT, "Output")

    override fun onEnable() {
        + inputMat
        + contours

        + lineColor
        + lineThickness

        lineThickness.value.set(1)

        if(!isDrawOnInput) {
            + outputMat
        }
    }

    override fun genCode(current: CodeGen.Current) = current {
        val session = Session()

        val color = lineColor.value(current)
        val colorScalar = tryName("contoursColor")

        val input = inputMat.value(current)
        val contoursList = contours.value(current)
        val thickness = lineThickness.value(current).value

        val output = tryName("${input.value.value!!}Contours")

        if(contoursList !is GenValue.RuntimeListOf<*>) {
            contours.raise("")
        }

        var drawMat = input.value

        // add necessary imports
        import("org.opencv.imgproc.Imgproc")
        import("org.opencv.core.Scalar")

        group {
            public(
                colorScalar,
                new(
                    "Scalar",
                    color.a.toString(),
                    color.b.toString(),
                    color.c.toString(),
                    color.d.toString(),
                )
            )

            if (!isDrawOnInput) {
                private(output, new("Mat"))
            }
        }

        current.scope {
            if(!isDrawOnInput) {
                drawMat = output.v
                "${input.value.value}.copyTo"(drawMat)
            }

            "Imgproc.drawContours"(
                drawMat,
                (contoursList as GenValue.RuntimeListOf<*>).value,
                (-1).v, colorScalar.v, thickness.v
            )
        }

        session.outputMat = GenValue.Mat(drawMat, input.color, input.isBinary)

        session
    }

    override fun getOutputValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        if(attrib == outputMat) {
            return genSession!!.outputMat
        }

        noValue(attrib)
    }

    class Session : CodeGenSession {
        lateinit var outputMat: GenValue.Mat
    }

}

@RegisterNode(
    name = "Draw Contours on Image",
    category = Category.OVERLAY,
    description = "Draws the contours list in the passed image."
)
class DrawContoursOnImageNode : DrawContoursNode("Draw Contours on Image", true)