package pl.spark.learning.s3

import java.io.File
import java.text.DecimalFormat
import java.util.concurrent.atomic.AtomicInteger

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.event.{ProgressEvent, ProgressEventType, ProgressListener}
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.PutObjectRequest
import com.amazonaws.services.s3.transfer.TransferManagerBuilder
import com.amazonaws.{AmazonServiceException, SdkClientException}
import com.google.common.util.concurrent.AtomicDouble
import pl.spark.learning.ResourceHelper

object MultipartUploader {

  def main(args: Array[String]): Unit = {
    val doubleFormat = new DecimalFormat("#.00")
    val bucketName = "spark.example.bucket"
    val keyName = "randomtext.txt"
    val filePath = ResourceHelper.getResourceFilepath(keyName)

    def pretty(number: Double): String = doubleFormat.format(number)

    try {
      val s3Client = AmazonS3ClientBuilder.standard
        .withRegion(Regions.US_EAST_1)
        .withCredentials(new DefaultAWSCredentialsProviderChain)
        .build

      val tm = TransferManagerBuilder.standard
        .withS3Client(s3Client)
        .withMultipartUploadThreshold((5 * 1024 * 1025).toLong)
        .build

      val fileToUpload = new File(filePath)
      val fileSizeMB = 1.0 * fileToUpload.length() / (1024 * 1024)
      val transferredMB = new AtomicDouble(0)
      val counter = new AtomicInteger(0)

      def printTransferredAlready(progressEvent: ProgressEvent): Unit = {
        transferredMB.addAndGet(1.0 * progressEvent.getBytesTransferred / 1000000)
        counter.incrementAndGet()

        if (counter.get() % 100 == 0)
          println("Transferred " + pretty(transferredMB.get()) + "MB of " + pretty(fileSizeMB) + "MB")
      }

      val progressListener = new ProgressListener {
        override def progressChanged(progressEvent: ProgressEvent): Unit = {
          progressEvent.getEventType match {
            case ProgressEventType.REQUEST_BYTE_TRANSFER_EVENT => printTransferredAlready(progressEvent)
          }
        }
      }

      val request = new PutObjectRequest(bucketName, keyName, fileToUpload)
      request.setGeneralProgressListener(progressListener)

      // TransferManager processes all transfers asynchronously,
      // so this call returns immediately.
      val upload = tm.upload(request)
      println("Object upload started")

      // Optionally, wait for the upload to finish before continuing.
      upload.waitForCompletion()
      println("Object upload complete")
    } catch {
      case e: AmazonServiceException => e.printStackTrace()
      case e: SdkClientException => e.printStackTrace()
    } finally System.exit(0)
  }
}
