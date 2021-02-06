# prism

**The official repository for the prism code is https://code.usgs.gov/prism/prism_engine/**

A continually increasing number of high-quality digital strong-motion records from stations of the National Strong Motion Project (NSMP) of the U.S. Geological Survey (USGS), as well as data from regional seismic networks within the U.S., called for automated processing of strong-motion records with human review limited to selected significant or flagged records. This PRISM (Processing and Review Interface for Strong Motion data) repository contains the software for the automated record processing engine, designed to perform data processing on earthquake sensor data, transforming raw sensor counts into acceleration, velocity, and displacement information.

PRISM is platform-independent, coded in Java, and open-source. To support use by earthquake engineers and scientists, PRISM is easy to install and run as a stand-alone system on common operating systems such as Linux, OS X and Windows.

The source code contains some additional NetBeans files to facilitate building code outside of an IDE or to bring the code into the NetBeans IDE. PRISM was developed with NetBeans version 8.0. The main file is Prism.java in the src/SmControl package. PRISM was developed in Java 1.8. There is a set of JavaDocs to accompany the code.

For more information, please visit https://earthquake.usgs.gov/research/software/prism/

**PRISM processing engine versions**

Version 1.0.0	(3/1/2017)
- Initial public release of code and documentation

Version 1.0.1	(3/30/2017)
- Minor update to cosmos headers

Version 1.0.2	(9/14/2017)	
- Improved resolution of data array in output files
- Expanded API for trim feature in Review Tool

Version 1.0.3   (10/16/2017)
- Improved error handling during adaptive baseline correction

Version 2.0.0	(10/2/2019)
- Interpolation of all data with SPS < 200 to SPS >= 200 for processing (SPS, samples per second)
- Added flag in config file to request interpolated results be decimated back to original value (on / off)
- Despiking algorithm added to V1 processing, with option to turn on or off in configuration file
- Fourier selection of filter corners option, selectable by flag in the configuration file
- Integration in time or frequency domain, selectable by flag in the configuration file
- Full or brief output version of the apktable, selectable by flag in the configuration file
- Check for minimum SNR, with cutoff value selectable in the configuration file

Version 2.0.1	(11/12/2019)
- Bug fix for cosine taper at end of filtering

Version 2.1.0   (8/11/2020)
- Added the filter order (from the configuration file) to the cosmos header for V2, V3 files
- Bug fix in apktable - set all Sa output units to g
- Copy V0 files that fail during reading into a new, high-level trouble folder
- Lower SPS for filter corner selection from 60 to 50
- Added station filter table to config file so filter corners could be read from a table based on SNCL
- Added prism version to the comments in the cosmos output files
- Added PGA threshold flag to configuration file to check for minimum PGA, with flag to disable
