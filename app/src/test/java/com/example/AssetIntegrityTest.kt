package com.example

import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.Font
import java.io.File
import javax.imageio.ImageIO

class AssetIntegrityTest {

  private fun resDir(): File {
    val candidates = listOf(
      File("app/src/main/res"),
      File("src/main/res")
    )
    return candidates.first { it.exists() }
  }

  @Test
  fun `font files are valid truetype fonts`() {
    val fontDir = File(resDir(), "font")
    assertTrue("font dir missing", fontDir.exists())
    val ttfFiles = fontDir.listFiles { f -> f.extension == "ttf" }.orEmpty()
    assertTrue("No ttf files found", ttfFiles.isNotEmpty())
    ttfFiles.forEach { file ->
      assertTrue("Font ${file.name} too small (${file.length()})", file.length() > 1000)
      Font.createFont(Font.TRUETYPE_FONT, file)
    }
  }

  @Test
  fun `no oversized raster images live in density scaled drawable folder`() {
    val drawableDir = File(resDir(), "drawable")
    assertTrue("drawable dir missing", drawableDir.exists())
    val rasterFiles = drawableDir.listFiles { f ->
      f.extension.lowercase() in setOf("jpg", "jpeg", "png", "webp")
    }.orEmpty()
    rasterFiles.forEach { file ->
      val img = ImageIO.read(file)
      assertTrue(
        "Raster image ${file.name} (${img?.width}x${img?.height}) should be in drawable-nodpi to prevent 3x/4x upscaling OOM/GL texture crashes",
        img != null && img.width <= 512 && img.height <= 512
      )
    }
  }

  @Test
  fun `drawable nodpi images decode cleanly within safe texture bounds`() {
    val nodpiDir = File(resDir(), "drawable-nodpi")
    assertTrue("drawable-nodpi dir missing", nodpiDir.exists())
    val images = nodpiDir.listFiles { f ->
      f.extension.lowercase() in setOf("jpg", "jpeg", "png", "webp")
    }.orEmpty()
    assertTrue("No raster images found in drawable-nodpi", images.size >= 5)
    images.forEach { file ->
      val img = ImageIO.read(file)
      assertTrue("Image ${file.name} failed to decode", img != null)
      assertTrue(
        "Image ${file.name} width=${img!!.width} height=${img.height} exceeds 2048px",
        img.width <= 2048 && img.height <= 2048
      )
    }
  }

  @Test
  fun `notification small icon vector exists`() {
    val iconFile = File(resDir(), "drawable/ic_stat_learning.xml")
    assertTrue("ic_stat_learning.xml missing", iconFile.exists())
    val content = iconFile.readText()
    assertTrue("Notification icon must be a vector drawable", content.contains("<vector"))
  }
}
