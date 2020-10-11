package gui;

import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.text.DecimalFormat;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.FileImageOutputStream;
import javax.swing.ImageIcon;

import org.apache.commons.imaging.ImageFormats;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
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
//          image = Imaging.getBufferedImage(file); //different library, not sure if different, use if there are problems
        }
        catch (Exception e)
        {
        	//failed to make image, handle exception here
          e.printStackTrace();
          System.exit(1);
        }
        Graphics2D gImage = image.createGraphics();
        
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
        //Construct GPS Coord String
        DecimalFormat df = new DecimalFormat("0.00000");
        String coords = null;
        try {
			coords = df.format(Math.abs(gpsInfo.getLatitudeAsDegreesNorth())) + "° " + gpsInfo.latitudeRef + ", " + df.format(Math.abs(gpsInfo.getLongitudeAsDegreesEast())) + "° " + gpsInfo.longitudeRef;
		} catch (ImageReadException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        
        //print gps and date info to SYSO for debugging reasons, remove in final release. TODO remove when done
        System.out.println(jpegMetadata.findEXIFValueWithExactMatch(TiffTagConstants.TIFF_TAG_DATE_TIME).getValueDescription());
        System.out.println(gpsInfo.latitudeDegrees + "° " + gpsInfo.latitudeMinutes + "\' " + gpsInfo.latitudeSeconds.doubleValue() + "\" " + gpsInfo.latitudeRef);
        System.out.println(gpsInfo.longitudeDegrees + "° " + gpsInfo.longitudeMinutes + "\' " + gpsInfo.longitudeSeconds.doubleValue() + "\" " + gpsInfo.longitudeRef);
        System.out.println(coords);
        
        
        
        TiffOutputSet outputSet = null;
        try {
			outputSet = exif.getOutputSet();
		} catch (ImageWriteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//        TiffOutputDirectory gpsCoords=null;
//        gpsCoords = outputSet.getGPSDirectory();
//        
//        System.out.println(gpsCoords.findField(2));
//        System.out.println(gpsCoords.findField(4));
//        TiffOutputField field = gpsCoords.findField(2);
        
        //setup font stuff so we can write on the image
        Font overlayFont = new Font(Font.SANS_SERIF,Font.PLAIN,60);
        gImage.setFont(overlayFont);
        //create shape of the outer edge of the text. This will allow us to draw the outline in one color and then fill with solid color
        TextLayout coordLayout = new TextLayout(coords,overlayFont,new FontRenderContext(null,false,false)); //turn string into style we want for rendering
        AffineTransform transform = new AffineTransform();
        transform.translate(100, 100);//set location of outline for rendering
        Shape outline = coordLayout.getOutline(transform); //get the outer edges of the text
        //Draw a string on the image
        //gImage.drawString(coords, 100, 100);//string does not quite line up if you use this, using fill instead.
        gImage.setColor(Color.BLACK);
        gImage.setStroke(new BasicStroke(2));
        gImage.draw(outline);//adds the outline
        gImage.setColor(Color.YELLOW);
        gImage.fill(outline); //fill in the outline with color
        
        //save image
        returnVal = fc.showOpenDialog(frame);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
            file = fc.getSelectedFile();
            //anything else we want to do with the file
        } else {
            //action on cancel, probably nothing
        }
		
		//create and image writer and set it to the quality settings we want
		final ImageWriter imWrite = ImageIO.getImageWritersByFormatName("jpg").next();
		JPEGImageWriteParam jpgParams = new JPEGImageWriteParam(null);
		jpgParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		jpgParams.setCompressionQuality(0.95f);//set quality to 90% using float
		
		//set location for writer by turning our file into an output stream and passing that to setOutput so we can close it later.
		FileImageOutputStream fileOutStream = null;
		try {
			fileOutStream = new FileImageOutputStream(file);
		} catch (IOException e4) {
			// TODO Auto-generated catch block
			e4.printStackTrace();
		}
		imWrite.setOutput(fileOutStream);
        try {
        	imWrite.write(null,new IIOImage(image,null,null),jpgParams);//write image using params set above. Still need to add EXIF data
        	fileOutStream.close();//close the file to release locks
//			ImageIO.write(image, "jpg", file); //TODO this method blows away existing EXIF data, find way to preserve. Write exif back to image after making?
//			Imaging.writeImage(image, file, ImageFormats.JPEG, null); //library does not support JPG writing so can not use this
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
        
        //add EXIF data using Apache library
        try {
			new ExifRewriter().updateExifMetadataLossless(Files.readAllBytes(file.toPath()), new FileOutputStream(file), outputSet);
		} catch (ImageReadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ImageWriteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
	}

}
