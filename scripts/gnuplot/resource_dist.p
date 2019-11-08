set term pdfcairo enhanced
set output 'resource_dist.pdf'
set style fill transparent solid 0.5 noborder
#set output '| ps2pdf - ./resource_dist.pdf'
set key outside right top vertical Right noreverse enhanced autotitles nobox
set title "plot with filledcurve [options]" 
plot [-10:10] [-5:3]    1.5+sin(x)/x with filledcurve x2,   sin(x)/x with filledcurve,    1+sin(x)/x with lines,  -1+sin(x)/x with filledcurve y1=-2,    -2.5+sin(x)/x with filledcurve xy=-5,-4.,    -4.3+sin(x)/x with filledcurve x1,  (x>3.5 ? x/3-3 : 1/0) with filledcurve y2
