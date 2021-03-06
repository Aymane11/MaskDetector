package com.maskdetector.detection.env;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.media.Image;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;

import com.maskdetector.BuildConfig;

import java.nio.ByteBuffer;

public final class YuvToRgbConverter {
    private final RenderScript renderScript;
    private final ScriptIntrinsicYuvToRGB scriptIntrinsicYuvToRGB;

    private ByteBuffer yuvBuffer;
    private Allocation inputAllocation;
    private Allocation outputAllocation;
    private Integer pixelCount = -1;

    public YuvToRgbConverter(Context context) {
        this.renderScript = RenderScript.create(context);
        this.scriptIntrinsicYuvToRGB = ScriptIntrinsicYuvToRGB.create(renderScript, Element.U8_4(renderScript));
    }

    public synchronized void yuvToRgb(Image image, Bitmap outputBitmap) throws Exception {
        if (yuvBuffer == null) {
            pixelCount = image.getCropRect().width() * image.getCropRect().height();
            int pixelSizeBits = ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888);
            yuvBuffer = ByteBuffer.allocateDirect(pixelCount * pixelSizeBits / 8);
        }

        yuvBuffer.rewind();
        imageToByteBuffer(image, yuvBuffer.array());

        if (inputAllocation == null) {
            Type elemType = new Type.Builder(renderScript, Element.YUV(renderScript))
                .setYuvFormat(ImageFormat.NV21)
                .create();
            inputAllocation = Allocation.createSized(renderScript,
                elemType.getElement(),
                yuvBuffer.array().length
            );
        }

        if (outputAllocation == null) {
            outputAllocation = Allocation.createFromBitmap(renderScript, outputBitmap);
        }

        inputAllocation.copyFrom(yuvBuffer.array());
        scriptIntrinsicYuvToRGB.setInput(inputAllocation);
        scriptIntrinsicYuvToRGB.forEach(outputAllocation);
        outputAllocation.copyTo(outputBitmap);
    }

    private void imageToByteBuffer(Image image, byte[] outputBuffer) throws Exception {
        if (BuildConfig.DEBUG && image.getFormat() != ImageFormat.YUV_420_888) {
            throw new Exception("Assertion Failure");
        }

        Rect imageCrop = image.getCropRect();
        Image.Plane[] imagePlanes = image.getPlanes();
        for (int planeIndex = 0; planeIndex < imagePlanes.length; planeIndex++) {
            int outputStride;
            int outputOffset;

            switch (planeIndex) {
                case 0:
                    outputStride = 1;
                    outputOffset = 0;
                    break;
                case 1:
                    outputStride = 2;
                    outputOffset = pixelCount + 1;
                    break;
                case 2:
                    outputStride = 2;
                    outputOffset = pixelCount;
                    break;
                default:
                    continue;
            }

            ByteBuffer planeBuffer = imagePlanes[planeIndex].getBuffer();
            int rowStride = imagePlanes[planeIndex].getRowStride();
            int pixelStride = imagePlanes[planeIndex].getPixelStride();
            Rect planeCrop = planeIndex == 0 ?
                imageCrop :
                new Rect(
                    imageCrop.left / 2,
                    imageCrop.top / 2,
                    imageCrop.right / 2,
                    imageCrop.bottom / 2
                );
            int planeWidth = planeCrop.width();
            int planeHeight = planeCrop.height();
            byte[] rowBuffer = new byte[rowStride];
            int rowLength = (pixelStride == 1 && outputStride == 1) ?
                planeWidth :
                (planeWidth - 1) * pixelStride + 1;

            for (int row = 0; row < planeHeight; row++) {
                planeBuffer.position(
                (row + planeCrop.top) * rowStride + planeCrop.left * pixelStride
                );

                if (pixelStride == 1 && outputStride == 1) {
                    planeBuffer.get(outputBuffer, outputOffset, rowLength);
                    outputOffset += rowLength;
                } else {
                    planeBuffer.get(rowBuffer, 0, rowLength);
                    for (int col = 0; col < planeWidth; col++) {
                        outputBuffer[outputOffset] = rowBuffer[col * pixelStride];
                        outputOffset += outputStride;
                    }
                }
            }
        }
    }
}
