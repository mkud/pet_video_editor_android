#LD_LIBRARY_PATH=/usr/local/lib ./videostab VID_20171024_153728.mp4 -r=100 -m=homography -et=no -o=./VID.avi -q
beg=`echo $(($(date +%s%N)/1000000))`
#LD_LIBRARY_PATH=/usr/local/lib ./videostab VID_20171024_153728.mp4 -r=180 -m=homography -et=no -o=./VID.avi -q
LD_LIBRARY_PATH=/usr/local/lib ./videostab ./cigrx.mp4 -r=180 -m=homography -o=./VID.avi -q
end=`echo $(($(date +%s%N)/1000000))`
echo $((end-beg))
#LD_LIBRARY_PATH=/usr/local/lib ./videostab VID_20170929_143159.mp4 -r=500 -m=homography -o=./VID.avi -q --deblur=yes
#affine 0.5 sec less 11 vs 10.5
# similarity similar
# rigid similar
#transl_and_scale similar



