# Automatic-Number-Plate-Detection
A number plate detection program witten in JAVA. Detect number plate from images using pixel values and width/height ratio of number plate.
  
  
## Implementation steps:  
1) Convert from RGB to Grayscale  
2) Apply median filter to smooth image  
3) Apply sobel edge detection  
4) Compute and smooth edge intensity(vertical projection)
5) Locate band candidates
6) Locate number plate in band candidates by pixel value and width/height ratio
7) Select most favorable band as final output 
  
  
## JAVA Files:
**ANPR.java:** JAVA file to detect number plate from an image.  
**ANPR_multi.java:** JAVA file to detect number plate from all images in the 'vehicle_images' folder.
  
  
## Others:
**output_img:** A folder containing output images of ANPR.java  
**results_img:** A folder containing output images of ANPR_multi.java
**vehicls_img:** A folder containing example images for the program
