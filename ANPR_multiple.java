import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.DataBuffer;
import java.awt.image.Kernel;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;


public class ANPR_multiple {

	public static void main(String[] args) {
		System.out.println("Program started...\n");
		
		for (int j=1; j<=97; j++) {
			System.out.print("Processing image "+j+"...");
			try {
				String imgfile = "vehicles_img/vehicle("+j+").jpg";
				BufferedImage img = ImageIO.read(new File(imgfile));
				
				// Convert from rgb to grayscale
				RGBToGrayscale(img);
				//displayImage(img,"Original Image");
				saveImage(img,"gray.jpg");
				
				// Apply median filter
				BufferedImage img_filtered = img;
				applyMedianFilter(img,img_filtered);		
				//displayImage(img_filtered,"Median Filter");
				saveImage(img_filtered,"medianFilter.jpg");
				
				// Apply sobel vertical edge detection
				BufferedImage edge_vertical = new BufferedImage(img.getWidth(),img.getHeight(),BufferedImage.TYPE_BYTE_GRAY);
				applySobel(img_filtered,edge_vertical,"Vertical");
				//displayImage(edge_vertical,"Vertical Edge");
				saveImage(edge_vertical,"verticalEdge.jpg");
				
				// Compute vertical projection
				int[] magnitude = new int[img.getHeight()];
				int[] magnitude_smoothed = new int[img.getHeight()];
				computeVerticalProjection(edge_vertical,magnitude,magnitude_smoothed);
				//plotGraph(magnitude,"Vertical Projection");
				//plotGraph(magnitude_smoothed,"Smoothed Vertical Projection");
				
				// Compute band candidates
				int candidate_num = 3;
				int[][] bands = new int[candidate_num][2]; 
				int[] magnitude_zeroized = new int[magnitude.length];
				magnitude_zeroized = computeBandCandidates(magnitude_smoothed,bands,candidate_num);
				//plotGraph(magnitude_zeroized,"Zeroized Smoothed Vertical Projection");
				
				// Crop selected bands from original image
				ArrayList<Integer> band_size = new ArrayList<Integer>();
				for (int i=0; i<candidate_num; i++) {
					BufferedImage original_img = ImageIO.read(new File(imgfile));
					if (bands[i][0]==0 && bands[i][1]==0) {
						candidate_num = i;
						break;
					}
					else {
						if (bands[i][0]+(bands[i][1]-bands[i][0]+15) < img.getHeight()) {
							BufferedImage candidate = original_img.getSubimage(0, bands[i][0], original_img.getWidth(), (bands[i][1]-bands[i][0]+15));
							band_size.add((bands[i][1]-bands[i][0]+15)-bands[i][0]);
							//displayImage(candidate,"candidate_"+(i+1));
							saveImage(candidate,"candidate_"+(i+1)+".jpg");
						}
						else {
							int remaining = img.getHeight() - bands[i][0];
							BufferedImage candidate = original_img.getSubimage(0, bands[i][0], original_img.getWidth(), remaining);
							band_size.add(remaining-bands[i][0]);
							//displayImage(candidate,"candidate_"+(i+1));
							saveImage(candidate,"candidate_"+(i+1)+".jpg");
						}
					}
				}
				
				// Band Selection (Compute ratio of rectangle)
				double[][] candidate_result = new double[candidate_num][5];
				
				for (int i=0; i<candidate_num; i++) {
					String bandfile = "output_img/candidate_"+(i+1)+".jpg";
					BufferedImage band = ImageIO.read(new File(bandfile));
					
					// convert to grayscale
					RGBToGrayscale(band);
					
					// apply median filter
					BufferedImage band_filtered = band;
					applyMedianFilter(band,band_filtered);
					
					// apply sobel edge detection
					BufferedImage band_sobel = new BufferedImage(band.getWidth(),band.getHeight(),BufferedImage.TYPE_BYTE_GRAY);
					applySobel(band_filtered,band_sobel,"Sobel");
					//displayImage(band_sobel,"candidate"+(i+1)+"_sobel");
					saveImage(band_sobel,"candidate"+(i+1)+"_sobel.jpg");
					
					// compute ratio of rectangle in band
					BufferedImage img_sobel = ImageIO.read(new File("output_img/candidate"+(i+1)+"_sobel.jpg"));
					candidate_result[i] = computeRectangleRatio(img_sobel);
				}
				
				// display final output
				// compute sum for later use (check if rectangle exists)
				int sum = 0;
				for (int i=0; i<candidate_num; i++) 
					sum+=candidate_result[i][0];
				
					while (true) {	
						// find smallest band
						int min = Integer.MAX_VALUE;
						int index = 0;
						for (int i=0; i<candidate_num; i++) {
							if (band_size.get(i)<min) {
								min = band_size.get(i);
								index = i;
							}
						}
						band_size.set(index, 100);
						
						// instantiate smallest band image
						String smallestBandFile = "output_img/candidate_"+(index+1)+".jpg";
						BufferedImage smallest_band = ImageIO.read(new File(smallestBandFile));
						
						// if rectangle exists in at least one band
						if (sum!=candidate_num*100) {
							if (candidate_result[index][0] < 1.5) { 
								//displayImage(smallest_band,"Final Output(Band)");
								saveResultImage(smallest_band,"vehicle("+j+")_band.jpg");
										
								BufferedImage crop = smallest_band.getSubimage((int)candidate_result[index][1], (int)candidate_result[index][2], (int)candidate_result[index][3], 
					                            							 (int)candidate_result[index][4]);
								//displayImage(crop,"Final Output(Cropped Band)");
								saveResultImage(crop,"vehicle("+j+")_crop.jpg");
								break;
							}
							else if (min >= 1.5) {
								//displayImage(smallest_band,"Final Output(Band)");
								saveResultImage(smallest_band,"vehicle("+j+")_band.jpg");
										
								BufferedImage crop = smallest_band.getSubimage((int)candidate_result[index][1], (int)candidate_result[index][2], (int)candidate_result[index][3], 
					                            							 (int)candidate_result[index][4]);
								//displayImage(crop,"Final Output(Cropped Band)");
								saveResultImage(crop,"vehicle("+j+")_crop.jpg");
								break;
							}
						}
						// if rectangle does not exist in any band
						else {
							//displayImage(smallest_band,"Final Output(Band)");
							saveResultImage(smallest_band,"vehicle("+j+")_band.jpg");
							break;
						}
					}
					System.out.println("DONE");
			}
			
			catch (Exception e) {
				System.out.print("FAILED: ");
				System.out.println(e);
			}
		}
		System.out.println("Result images is stored in 'output_img' folder of the workspace directory.");
	}
	
	
	
	public static void RGBToGrayscale(BufferedImage img) {
			
			// get image width and height
			int width = img.getWidth();
			int height = img.getHeight();
			int maxWidth = width-1;
			int maxHeight = height-1;

			// convert to grayscale
			for(int y = 0; y < height; y++){
				for(int x = 0; x < width; x++){
					int p = img.getRGB(x,y);

					int a = (p>>24)&0xff;
					int r = (p>>16)&0xff;
					int g = (p>>8)&0xff;
					int b = p&0xff;

					//calculate average
					int avg = (r+g+b)/3;

					//replace RGB value with avg
					p = (a<<24) | (avg<<16) | (avg<<8) | avg;

					img.setRGB(x, y, p);
				}
			}
	}
	
	
	public static void applyMedianFilter(BufferedImage source_img, BufferedImage target_img) {
		
		// get image width and height
		int width = source_img.getWidth();
		int height = source_img.getHeight();
		int maxWidth = width-1;
		int maxHeight = height-1;
		
		// apply median filter
		int[][][] coor = new int[3][3][2];
		int[] kernel = new int[9];
		for (int h = 0; h < height; h++) {
			for (int w = 0; w < width; w++) {	
				// get x,y coordinates of all 3x3 neighbouring pixels
				coor[0][0][0] = (w==0)? w: w-1; 		coor[0][0][1] = (h==0)? h: h-1;
				coor[0][1][0] = w; 						coor[0][1][1] = (h==0)? h: h-1;
				coor[0][2][0] = (w==maxWidth)? w: w+1; 	coor[0][2][1] = (h==0)? h: h-1; 
				coor[1][0][0] = (w==0)? w: w-1;			coor[1][0][1] = h;
				coor[1][1][0] = w;						coor[1][1][1] = h;
				coor[1][2][0] = (w==maxWidth)? w: w+1;	coor[1][2][1] = h;
				coor[2][0][0] = (w==0)? w: w-1;			coor[2][0][1] = (h==maxHeight)? h: h+1; 
				coor[2][1][0] = w;						coor[2][1][1] = (h==maxHeight)? h: h+1;
				coor[2][2][0] = (w==maxWidth)? w: w+1;	coor[2][2][1] = (h==maxHeight)? h: h+1; 
				
				// get values for all 3x3 neighbouring pixels from img
				int index = 0;
				for (int i = 0; i < 3; i++) {
					for (int j = 0; j < 3; j ++) 
						kernel[index++] = source_img.getRGB(coor[i][j][0],coor[i][j][1]) &0xff;
				}
				
				// get median
				Arrays.sort(kernel);
				int median = kernel[4];
				
				// set median value to img_gray
				int a = (source_img.getRGB(w, h)>>24) &0xff;
				int p = (a<<24) | (median<<16) | (median<<8) | median;
				target_img.setRGB(w, h, p);
			}
		}
	}
	
	
	public static void applySobel(BufferedImage source_img, BufferedImage target_img, String type) {
		
		// initialize important variables
		int width = source_img.getWidth();
		int height = source_img.getHeight();
		final float[] SOBEL_V = {1,0,-1,2,0,-2,1,0,-1};
		final float[] SOBEL_H = {1,2,1,0,0,0,-1,-2,-1};
		BufferedImage edge_vertical = new BufferedImage(width,height,BufferedImage.TYPE_BYTE_GRAY);   // For vertical edge (type=vertical) 
		BufferedImage edge_horizontal = new BufferedImage(width,height,BufferedImage.TYPE_BYTE_GRAY); // For horizontal edge (type=horizontal)
		int maxGradient = -1; // Only used in sobel													  // For vertical and horizontal (type=sobel), use target_img directly
		int[][] edges = new int[width][height]; // Only used in sobel
		ConvolveOp convolution; // Only used in vertical and horizontal
		
		
		// perform convolution between gray values of image and kernel
		if (type.toLowerCase().equals("vertical") || type.toLowerCase().equals("sobel")) {
			convolution = new ConvolveOp(new Kernel(3,3,SOBEL_V));
			if (type.toLowerCase().equals("vertical"))
				convolution.filter(source_img,target_img); // if type is vertical, convolve on target image directly
			else {
				convolution.filter(source_img,edge_vertical);
			}
		}
		if (type.toLowerCase().equals("horizontal") || type.toLowerCase().equals("sobel")) {
			convolution = new ConvolveOp(new Kernel(3,3,SOBEL_H));
			if (type.toLowerCase().equals("horizontal"))
				convolution.filter(source_img,target_img); // if type is horizontal, convolve on target image directly
			else {
				convolution.filter(source_img,edge_horizontal);
			}
		}
		if (type.toLowerCase().equals("sobel")) {

			// get pixel values for both vertical and horizontal edge
			for(int y = 0; y < height; y++){
				for(int x = 0; x < width; x++){
					int p1 = edge_vertical.getRGB(x,y);
					int a1 = (p1>>24)&0xff;
					int r1 = (p1>>16)&0xff;
						
					int p2 = edge_horizontal.getRGB(x,y);
					int r2 = (p2>>16)&0xff;
					
					double gval = Math.sqrt((r1 * r1) + (r2 * r2));
					int g = (int) gval;
					
					// get maximum gradient for scaling
	                if(maxGradient < g) {
	                    maxGradient = g;
	                }    
	                edges[x][y] = g;
				}
			}
			
			// scale the overall intensity of edges
			double scale = 255.0 / maxGradient;
			
			// set pixel value for output image after scaling the edge intensity
	        for (int i = 0; i < width ; i++) {
	            for (int j = 0; j < height; j++) {
	                int edgeColor = edges[i][j];
	                edgeColor = (int)(edgeColor * scale);
	                edgeColor = 0xff000000 | (edgeColor << 16) | (edgeColor << 8) | edgeColor;

	                target_img.setRGB(i, j, edgeColor);
	            }
	        }
		}
	}
	
	
	public static void computeVerticalProjection(BufferedImage img, int[] magnitude, int[] magnitude_smoothed) {
		
		int width = img.getWidth();
		int height = img.getHeight();
		
		// calculate vertical projection
		for (int h=0; h<height; h++) {
			for (int w=0; w<width; w++) {
				int p = img.getRGB(w,h);
				int r = (p>>16)&0xff;
				magnitude[h] += r; 
			}
		}
		
		// use padding to maintain size of smoothed projection
		int[] magnitude_op = new int[height+8];
		for (int w=0; w<magnitude_op.length; w++) {
			if (w<=4 || w>=height)
				magnitude_op[w] = 0;
			else
				magnitude_op[w] = magnitude[w];
		}
		
		// compute median among 9 neighbour values
		for (int h=0; h<height; h++) {
			int sum = 0;
			for (int i=0; i<9; i++)
				sum += magnitude_op[h+i];
			magnitude_smoothed[h] = sum/9;
		}
	}
	
	
	public static int[] computeBandCandidates(int[] magnitude_smoothed, int[][]bands, int candidate_num) {
		
		int[] smoothed_magnitude_op = Arrays.copyOf(magnitude_smoothed, magnitude_smoothed.length);
		final double CONSTANT = 0.55;
		int y0,y1,max,index;
		
		for (int i=0; i<candidate_num; i++) {
			y0 = 0;
			y1 = 0;
			max = 0;
			index = 0;
			
			// find max & record the index
			for (int h=0; h<smoothed_magnitude_op.length; h++) 
				if (smoothed_magnitude_op[h] > max) {
					max = smoothed_magnitude_op[h];
					index = h;
				}	
			
			if (max!=0) { 
				// find y0
				for (int h=index; h>0; h--)
					if (smoothed_magnitude_op[h] <= (max*CONSTANT)) {
						y0 = h;
						break;
					}
				// find y1
				for (int h=index; h<smoothed_magnitude_op.length; h++)
					if (smoothed_magnitude_op[h] <= (max*CONSTANT)) {
						y1 = h;
						break;
					}
				
				// save y0 & y1 into list and zeroize interval <y0,y1>
				bands[i][0] = y0;
				bands[i][1] = y1;
				for (int h=y0; h<=y1; h++)
					smoothed_magnitude_op[h] = 0;	
				
			}
			else
				break;
		}
		return smoothed_magnitude_op;
	}
	
	
	public static double[] computeRectangleRatio(BufferedImage band) {
			
			//initialize important variables
			int width = band.getWidth();
			int height = band.getHeight();
			int start_index = 0;
			int end_index = 0;
			int rect_width = 0;
			int rect_height = 0;
			double ratio = 0;
			int numOfTraverseAllowed = 0;
			final int MIN_EDGE_INTENSITY = 100; // minimum intensity to be considered as number plate edges
			final int MIN_RECT_WIDTH = 70; 		// minimum width to be considered as number plate
			final int MIN_RECT_HEIGHT = 15; 	// minimum height to be considered as number plate
			double current_score = 100; 		// record the score for each current rectangle found within a band
			double score = 100; 				// lowest score indicates higher probability of true number plate, returned by function
			
			// Used to crop number plate, returned by function
			int crop_x = 0; 
			int crop_y = 0; 
			int crop_width = 0;
			int crop_height = 0;
			
			for (int y=0; y<height; y++) {
				rect_width = 0;
				for (int x=0; x<width; x++) {
					int p = band.getRGB(x,y);
					int r = (p>>16)&0xff;
					
					// if pixel value is greater than minimum edge intensity
					if (r>=MIN_EDGE_INTENSITY) {
						start_index = x;
						for (int l=start_index; l<width; l++) {
							int px = band.getRGB(l,y);
							int rx = (px>>16)&0xff;
							
							if (rx>=MIN_EDGE_INTENSITY) {
								rect_width++;
							}
							if (rx<MIN_EDGE_INTENSITY) {
								end_index = l;
								break;
							}
							if (l==width-1) {
								end_index = l;
								break;
							}
						}
						// width requirements met, now check for height requirements
						if (rect_width>=MIN_RECT_WIDTH) {
							numOfTraverseAllowed = rect_width - MIN_RECT_WIDTH;
							for (int l=start_index; l<(start_index+numOfTraverseAllowed); l++) {
								for (int h=y; h<height; h++) {
									int py = band.getRGB(l,h);
									int ry = (py>>16)&0xff;
									
									if (ry>=MIN_EDGE_INTENSITY) 
										rect_height++;
									if (ry<MIN_EDGE_INTENSITY) 
										break;
									if (h==height-1)
										break;
								}
								
								// height requirements met, calculate the score
								if (rect_height>=MIN_RECT_HEIGHT) {
									ratio = (double)rect_width/rect_height;
									current_score = Math.abs(ratio-5);
									if (current_score < score) {
										score = current_score;
										crop_x = l;
										crop_y = y;
										crop_width = rect_width;
										crop_height = rect_height;
									}
									x = end_index+1;
									rect_width = 0;
									rect_height = 0;
									break;
								}
								else {
									rect_height = 0;
								}
							}
							rect_width = 0;
							x = end_index+1;
						}
						// width requirements not met
						else {
							x = end_index+1;
							rect_width = 0;
						}
					}	
				}
			}
			double[] result = new double[] {score,crop_x,crop_y,crop_width,crop_height};
			return result;
	}
	
	
	public static void plotGraph(int[] magnitude, String title) {
		JFrame frame = new JFrame();
		frame.setTitle(title);
		Plot plot = new Plot(magnitude);
		frame.add(plot);
		frame.setSize(700,500);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}
	
	
	public static void displayImage(BufferedImage img, String title) {			
			JFrame frame = new JFrame();
			frame.setTitle(title);
			ImageIcon icon = new ImageIcon(img);
			JLabel label = new JLabel(icon);
			frame.add(label);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.pack();
			frame.setVisible(true);
	}
	
	
	public static void saveImage(BufferedImage img, String filename) {
		try {
		ImageIO.write(img, "jpg", new File("output_img/"+filename));
		}
		catch (Exception e) {
			System.out.println(e);
		}
	}
	
	
	public static void saveResultImage(BufferedImage img, String filename) {
		try {
		ImageIO.write(img, "jpg", new File("results_img/"+filename));
		}
		catch (Exception e) {
			System.out.println(e);
		}
	}

}

