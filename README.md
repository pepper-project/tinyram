# tinyram #

This is an independent reimplementation of ideas from vnTinyRAM, as
described in the publications listed below.

Note that the processor architecture we implement is somewhat different
from what's described in the above architecture spec; we call this
architecture "Lajos" to avoid confusion with (vn)TinyRAM.

If you have pandoc, LaTeX, and some ancillary packages available on your
system, you can `make` in the doc directory for an overview of our
architecture and a guide for using the executables.

To actually run verified computations on this system, you will need
to check out the latest
[Pepper system](https://github.com/pepper-project/pepper).
(Note also: this git project is a submodule of Pepper.)

## Related publications ##

Riad S. Wahby, Srinath Setty, Zuocheng Ren, Andrew J. Blumberg, and Michael Walfish.
"Efficient RAM and control flow in verifiable outsourced computation."
To appear in Network &amp; Distributed System Security Symposium,
[NDSS 2015](http://www.internetsociety.org/events/ndss-symposium-2015), February 2015.
http://www.pepper-project.org/buffet-ndss15.pdf
ePrint also available: https://eprint.iacr.org/2014/674

Ben-Sasson, E., Chiesa, A., Genkin, D., and Tromer, E.
"Fast reductions from RAMs to delegatable succinct constraint
satisfaction problems: extended abstract." ITCS 2013.
http://dl.acm.org/citation.cfm?id=2422481
ePrint also available: https://eprint.iacr.org/2012/071

Ben-Sasson, E., Chiesa, A., Genkin, D., Tromer, E., and Virza, M.
"SNARKs for C: Verifying program executions succinctly and in zero
knowledge." CRYPTO 2013.
ePrint also available: https://eprint.iacr.org/2013/507

Ben-Sasson, E., Chiesa, A., Tromer, E., and Virza, M.
"Succinct non-interactive zero knowledge for a von Neumann
architecture." USENIX Security 2014.
ePrint also available: https://eprint.iacr.org/2013/879

Ben-Sasson, E., Chiesa, A., Genkin, D., Tromer, E., and Virza, M.
"TinyRAM Architecture Specification v0.991."
http://www.scipr-lab.org/system/files/TinyRAM-spec-0.991.pdf
