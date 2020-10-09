package gui;

import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.BorderLayout;
import java.awt.Color;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.constants.GeoTiffTagConstants;
import org.apache.commons.imaging.formats.tiff.constants.TiffConstants;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputField;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;

public class AppWindow {

	private JFrame frame;
	//Create a file chooser
	final JFileChooser fc = new JFileChooser();

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					AppWindow window = new AppWindow();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public AppWindow() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 450, 300);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		
		//open file chooser
		File file = null;
		int returnVal = fc.showOpenDialog(frame);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
            file = fc.getSelectedFile();
            //anything else we want to do with the file
        } else {
            //action on cancel, probably nothing
        }
		
		BufferedImage image = null;
        try
        {
          image = ImageIO.read(file);
        }
        catch (Exception e)
        {
        	//failed to make image, handle exception here
          e.printStackTrace();
          System.exit(1);
        }
        Graphics2D gImage = image.createGraphics();
        
        //setup font stuff so we can write on the image
        Font overlayFont = new Font("Arial",Font.PLAIN,24);
        gImage.setFont(overlayFont);
        gImage.setColor(Color.YELLOW);
        //Draw a string on the image
        gImage.drawString("Test String", 100, 100);
        
        
        //save image
        returnVal = fc.showOpenDialog(frame);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
            file = fc.getSelectedFile();
            //anything else we want to do with the file
        } else {
            //action on cancel, probably nothing
        }
		//TODO this method blows away existing EXIF data, find way to preserve.
        try {
			ImageIO.write(image, "jpg", file);
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
        
        //get the byte array of the original file of the image so that we can use it directly to find the EXIF data. Assumes the image is a jpeg
        byte imageBytes[]=null;
		try {
			imageBytes = Files.readAllBytes(file.toPath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//use Apache library to calculate the metadata and convert it to a TIFF style EXIF 
		ImageMetadata metadata = null;
        try {
			metadata = Imaging.getMetadata(imageBytes);
		} catch (ImageReadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        final JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
        final TiffImageMetadata exif = jpegMetadata.getExif();
        
        //Extract GPS metadata from the image
        TiffImageMetadata.GPSInfo gpsInfo = null;
        try {
			gpsInfo = exif.getGPS();
		} catch (ImageReadException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        
        //print gps and date info to SYSO for debugging reasons, remove in final release. TODO remove when done
        System.out.println(jpegMetadata.findEXIFValueWithExactMatch(TiffTagConstants.TIFF_TAG_DATE_TIME).getValueDescription());
        System.out.println(gpsInfo.latitudeDegrees + "° " + gpsInfo.latitudeMinutes + "\' " + gpsInfo.latitudeSeconds.doubleValue() + "\" " + gpsInfo.latitudeRef);
        System.out.println(gpsInfo.longitudeDegrees + "° " + gpsInfo.longitudeMinutes + "\' " + gpsInfo.longitudeSeconds.doubleValue() + "\" " + gpsInfo.longitudeRef);
        System.out.println();
        
        
        
//        TiffOutputSet outputSet = null;
//        try {
//			outputSet = exif.getOutputSet();
//		} catch (ImageWriteException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//        TiffOutputDirectory gpsCoords=null;
//        gpsCoords = outputSet.getGPSDirectory();
//        
//        System.out.println(gpsCoords.findField(2));
//        System.out.println(gpsCoords.findField(4));
//        TiffOutputField field = gpsCoords.findField(2);
        
	}

}
