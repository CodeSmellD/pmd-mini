A Fork of the famous PMD, functionality reduced to focus on only Java smells.    
Tested thru IDE Execution.       

# Usage
1- Pull and import as Maven project to IDEA.    
2- Execute mvn clean install at project top level.    
3- Execute via PMD Main Class using params like: "-d /path/to/project  -R java-basic -f text".       
Detailed commandline tool params could be found in PMD Official Documentation.      
If NullPointerException related to Language.class occurred, try specify module classPath like the following screenshot.        

![config](https://wx2.sinaimg.cn/mw690/005yrqtrly1fuhdoe0kl0j31kw1d146v.jpg)

# Example Execution Command
-d /Users/bytenoob/Downloads/shopizer-2.2.0  -R java-exp -f csv -r /Users/bytenoob/Documents/javalibs/shopizer.csv