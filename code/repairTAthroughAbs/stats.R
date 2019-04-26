# Obs: It works only if the script is "Sourced" before the run.
# This can be done via the command: source('path/to/this/file/plots2.R')
# The suggestion is to use Rstudio as IDE and make sure to have the flag "Source on Save" checked.
# ( see: http://stackoverflow.com/questions/13672720/r-command-for-setting-working-directory-to-source-file-location )
# If it doesn't work because of UTF8, see: https://support.rstudio.com/hc/en-us/community/posts/200661587-Bug-when-sourcing-the-application?sort_by=votes 
this.dir <- dirname(parent.frame(2)$ofile)
setwd(this.dir)

library(ggplot2)
options(gsubfn.engine = "R") # Thanks to http://stackoverflow.com/questions/17128260/r-stuck-in-loading-sqldf-package
library(sqldf)
library(gridExtra)
library(gtable)
library(grid)
library(scales)
library(reshape2)
library(rPref)
library(xtable)
library(directlabels)
library(ggrepel)
#library(memisc)
library(ggthemes)
library(plotrix)
library(plyr)
#library(rescale)
library(dplyr)

folder <- './'
folderInput <- './files/'

# *********************************
# ******** UTILITY FUNCTIONS ******
# *********************************

f <- function(x) {
  if (x <= 9999 && x%%1==0 ) result <- as.integer(x+0.5)
  else {
    if (x<0.01 || x>9999) result <- sapply(strsplit(format(x, scientific=TRUE), split="e"), function(x) paste0("$", x[1], " \times 10^{", x[2], "}$"))
    else result <- as.integer(x*100+0.5)/100
  }
  return(result)
}

g <- function(x) {
  if (is.null(x) || is.na(x)) result <- "NA"
  else result <- paste("\\num{",x,"}",sep="")
  return(result)
}

optLegend <- theme(legend.direction = "horizontal", legend.justification = c(0, 1), legend.position = c(0.03,0.99))
noLegend <- theme(legend.position = "none", axis.title.x=element_blank(), axis.title.y=element_blank())
noAxisLabel <- theme(axis.title.x=element_blank(), axis.title.y=element_blank())
scaleGrey <- scale_fill_grey(start = 0.5, end = 1)

g_legend <- function(a.gplot){
  tmp <- ggplot_gtable(ggplot_build(a.gplot))
  leg <- which(sapply(tmp$grobs, function(x) x$name) == "guide-box")
  legend <- tmp$grobs[[leg]]
  return(legend)}
geo <- geom_point(size=2)
nl <- theme(legend.position="none")
bl <- theme(legend.direction = "horizontal", legend.position = "bottom")
sm <- geom_smooth(method=lm, se=FALSE) #see: http://www.sthda.com/english/wiki/ggplot2-scatter-plots-quick-start-guide-r-software-and-data-visualization

# *****************************
# ********** FUNCTIONS ********
# *****************************

# statistics about comparison experiments
statistics <- function() {
  print("Table 1")
  assign("dat", read.csv(paste(folderInput,"stats.txt",sep=""), encoding="UTF-8", row.names = NULL), envir=.GlobalEnv)
  
  #dat[dat == "RANDOM"] <- "policyrand"

  #colnames(dat) <- c(colnames(dat)[-1],NULL)
  #dat <<- transform(dat, correct = ifelse(correct == "true", 1, 0))
  #assign("benchmarkStats", read.csv(paste(folderInput,"benchmarkStats.txt",sep=""), encoding="UTF-8", row.names = NULL), envir=.GlobalEnv)
  #datMerge <<- merge(dat,benchmarkStats,by = "benchmark")
  
  depthMax <<- sqldf("select benchmark,mode,max(depth) as depth from dat group by benchmark,mode")
  datMerge <<- merge(dat,depthMax,by = c("benchmark","mode","depth"))
  
  
  # ONESHOT approach
  #datElab <<- sqldf("select benchmark,mode,depth,avg(time) as ttt,avg(unconformantTests) as utests,avg(mp+mnp) as tests,avg(distConstraintsAvg) as aDist,avg(distConstraintsVar) as vDist,avg(distFinal) as distTA from datMerge where generator like 'TestGeneratorFromStateFile' group by benchmark,mode,depth")
  datElab <<- sqldf("select benchmark,mode,depth,avg(time) as ttt,avg(unconformantTests) as utests,avg(mp+mnp) as tests,avg(distFinal) as distTA, timeGeneration,timeClassification,timeImitator,timeChoco,semFail,semTotal,semanticDistance from datMerge where generator like 'TestGeneratorFromStateFile' group by benchmark,mode,depth")
  datElab$ttt <- datElab$ttt/1000
  datElab$timeGeneration <- datElab$timeGeneration/1000
  datElab$timeClassification <- datElab$timeClassification/1000
  datElab$timeImitator <- datElab$timeImitator/1000
  datElab$timeChoco <- datElab$timeChoco/1000
  datTable2 <- format(datElab, digits=4)
  #datTable2 <- datElab
  #datTable <- sqldf("select benchmark,mode,ttt,(utests || '/' || tests) as tts,(aDist || ' +- ' || vDist) as distance,distTA from datTable2", method = "raw")
  datTable <<- sqldf("select benchmark,mode,ttt,timeGeneration,timeClassification,timeImitator,timeChoco, (utests || '/' || tests) as tts,distTA,semanticDistance from datTable2", method = "raw")
  # https://groups.google.com/forum/#!topic/sqldf/GZYTkDDyey4
  datTable$mode[datTable$mode == "MIN_VAL"] <- "policymin"
  datTable$mode[datTable$mode == "MAX_VAL"] <- "policymax"
  datTable$mode[datTable$mode == "MINUS1_EQUAL_PLUS1"] <- "policyminusplus"
  datTable$mode[datTable$mode == "MINUS1_EQUAL_PLUS1_MIDDLE"] <- "policymiddle"
  datTable$mode[datTable$mode == "MINUS1_EQUAL_PLUS1_QUARTER"] <- "policyquarter"
  datTable$mode[datTable$mode == "RANDOM"] <- "policyrand"
  print(xtable(datTable, type = "latex", display=rep("s",ncol(datTable)+1)), include.rownames=FALSE)
  
  datGraph <- sqldf("select * from datElab where benchmark <> \"runningExampleAlt\" ", method = "raw")
    # boxplot mode vs distTA
  datGraph$mode[datGraph$mode == "MIN_VAL"] <- "Pmin"
  datGraph$mode[datGraph$mode == "MAX_VAL"] <- "Pmax"
  datGraph$mode[datGraph$mode == "MINUS1_EQUAL_PLUS1"] <- "P1"
  datGraph$mode[datGraph$mode == "MINUS1_EQUAL_PLUS1_MIDDLE"] <- "PminMax2"
  datGraph$mode[datGraph$mode == "MINUS1_EQUAL_PLUS1_QUARTER"] <- "PminMax4"
  datGraph$mode[datGraph$mode == "RANDOM"] <- "Prnd"
  p1 <- ggplot(datGraph, aes(mode,distTA,fill=mode)) + geom_boxplot() + scale_fill_grey(start = 0.5, end = 1) + theme_bw() + noLegend #+ theme(axis.text.x=element_text(angle=0,hjust=1)) 
  print(p1)
  ggsave(p1, file="../papers/tap2019/images/mode_distTA.pdf", width=5, height=2.3)
  p1 <- ggplot(datGraph, aes(mode,tests,fill=mode)) + geom_boxplot() + scale_fill_grey(start = 0.5, end = 1) + theme_bw() + noLegend #+ theme(axis.text.x=element_text(angle=0,hjust=1)) 
  print(p1)
  ggsave(p1, file="../papers/tap2019/images/mode_tests.pdf", width=5, height=2.3)
  
  
  
  # ITERATIVE approach
  #datElab <<- sqldf("select benchmark,mode,depth,avg(time) as ttt,avg(unconformantTests) as utests,avg(mp+mnp) as tests,avg(distConstraintsAvg) as aDist,avg(distConstraintsVar) as vDist,avg(distFinal) as distTA from dat where generator like 'TestGeneratorJava' group by benchmark,mode,depth")
  #datTable2 <- format(datElab, digits=2)
  #datTable <- sqldf("select benchmark,mode,depth,ttt,(utests || '/' || tests) as tts,(aDist || ' +- ' || vDist) as distance,distTA from datTable2", method = "raw")
  # https://groups.google.com/forum/#!topic/sqldf/GZYTkDDyey4
  #print(xtable(datTable, type = "latex", display=rep("s",ncol(datTable)+1)), include.rownames=FALSE)
  
}
statistics()
